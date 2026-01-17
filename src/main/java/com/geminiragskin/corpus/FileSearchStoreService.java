package com.geminiragskin.corpus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing Gemini File Search Stores.
 * Handles store creation, file import, document management, and store deletion.
 */
@Service
public class FileSearchStoreService {

    private static final Logger logger = LoggerFactory.getLogger(FileSearchStoreService.class);
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta";

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public FileSearchStoreService() {
        this.webClient = WebClient.builder()
                .baseUrl(API_BASE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a new File Search Store.
     *
     * @param displayName The display name for the store
     * @return The store resource name (e.g., "stores/xyz123")
     * @throws IOException if store creation fails
     */
    public String createStore(String displayName) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Gemini API key not configured");
        }

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("displayName", displayName);

            String response = webClient.post()
                    .uri("/fileSearchStores?key=" + apiKey)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(response);
            String storeName = responseJson.path("name").asText();

            logger.info("File Search Store created successfully: {}", storeName);
            return storeName;

        } catch (Exception e) {
            logger.error("Failed to create File Search Store", e);
            throw new IOException("Failed to create File Search Store: " + e.getMessage(), e);
        }
    }

    /**
     * Imports a file into a File Search Store.
     * The file is automatically indexed and embedded for search.
     *
     * @param storeId The store resource name (e.g., "stores/xyz123")
     * @param file    The file to import
     * @return Document information containing the document ID
     * @throws IOException if import fails
     */
    public DocumentInfo importFileToStore(String storeId, MultipartFile file) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Gemini API key not configured");
        }

        try {
            String filename = file.getOriginalFilename();
            byte[] fileContent = file.getBytes();
            String mimeType = determineMimeType(filename);

            // Build multipart request
            MultipartBodyBuilder builder = new MultipartBodyBuilder();

            // Add file part
            builder.part("file", new ByteArrayResource(fileContent))
                    .filename(filename)
                    .header("Content-Type", mimeType);

            String response = webClient.post()
                    .uri("/fileSearchStores/{storeId}/documents?key=" + apiKey, storeId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(response);
            String documentName = responseJson.path("document").path("name").asText();
            String displayName = responseJson.path("document").path("displayName").asText();

            logger.info("File imported to store successfully: {} -> {} ({})",
                    filename, storeId, documentName);

            return new DocumentInfo(documentName, displayName, mimeType, file.getSize());

        } catch (Exception e) {
            logger.error("Failed to import file to store: {}", file.getOriginalFilename(), e);
            throw new IOException("Failed to import file to store: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all documents in a File Search Store.
     *
     * @param storeId The store resource name
     * @return List of documents in the store
     * @throws IOException if listing fails
     */
    public List<DocumentInfo> listDocuments(String storeId) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Gemini API key not configured");
        }

        try {
            String response = webClient.get()
                    .uri("/fileSearchStores/{storeId}/documents?key=" + apiKey, storeId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(response);
            JsonNode documentsArray = responseJson.path("documents");

            List<DocumentInfo> documents = new ArrayList<>();
            if (documentsArray.isArray()) {
                for (JsonNode docNode : documentsArray) {
                    String name = docNode.path("name").asText();
                    String displayName = docNode.path("displayName").asText();
                    String mimeType = docNode.path("mimeType").asText();
                    long sizeBytes = docNode.path("sizeBytes").asLong(0);

                    documents.add(new DocumentInfo(name, displayName, mimeType, sizeBytes));
                }
            }

            logger.info("Listed {} documents from store: {}", documents.size(), storeId);
            return documents;

        } catch (Exception e) {
            logger.error("Failed to list documents from store: {}", storeId, e);
            throw new IOException("Failed to list documents from store: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a document from a File Search Store.
     *
     * @param storeId    The store resource name
     * @param documentId The document resource name (e.g., "stores/xyz/documents/abc")
     * @throws IOException if deletion fails
     */
    public void deleteDocument(String storeId, String documentId) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Gemini API key not configured");
        }

        try {
            webClient.delete()
                    .uri("/fileSearchStores/{storeId}/documents/{documentId}?key=" + apiKey,
                            storeId, documentId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            logger.info("Document deleted from store: {} ({})", storeId, documentId);

        } catch (Exception e) {
            logger.error("Failed to delete document from store: {} ({})", storeId, documentId, e);
            throw new IOException("Failed to delete document from store: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an entire File Search Store and all its contents.
     *
     * @param storeId The store resource name
     * @throws IOException if deletion fails
     */
    public void deleteStore(String storeId) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Gemini API key not configured");
        }

        try {
            webClient.delete()
                    .uri("/fileSearchStores/{storeId}?key=" + apiKey, storeId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            logger.info("File Search Store deleted: {}", storeId);

        } catch (Exception e) {
            logger.error("Failed to delete File Search Store: {}", storeId, e);
            throw new IOException("Failed to delete File Search Store: " + e.getMessage(), e);
        }
    }

    /**
     * Determines the MIME type based on file extension.
     */
    private String determineMimeType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lower.endsWith(".doc")) {
            return "application/msword";
        } else if (lower.endsWith(".txt")) {
            return "text/plain";
        } else if (lower.endsWith(".md")) {
            return "text/markdown";
        } else if (lower.endsWith(".csv")) {
            return "text/csv";
        } else if (lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (lower.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lower.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else if (lower.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        } else if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "text/html";
        } else if (lower.endsWith(".xml")) {
            return "application/xml";
        } else if (lower.endsWith(".json")) {
            return "application/json";
        }
        return "application/octet-stream";
    }

    /**
     * Inner class to represent document information returned from File Search Store.
     */
    public static class DocumentInfo {
        private final String name;
        private final String displayName;
        private final String mimeType;
        private final long sizeBytes;

        public DocumentInfo(String name, String displayName, String mimeType, long sizeBytes) {
            this.name = name;
            this.displayName = displayName;
            this.mimeType = mimeType;
            this.sizeBytes = sizeBytes;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getMimeType() {
            return mimeType;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }
    }
}
