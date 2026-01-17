package com.geminiragskin.corpus;

import com.geminiragskin.file.FileInfo;
import com.geminiragskin.search.SearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for managing Gemini File Search Store operations.
 * Handles store creation, file import, and search queries using Gemini 2.5 File Search.
 */
@Service
public class GeminiCorpusService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiCorpusService.class);
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta";

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final FileSearchStoreService fileSearchStoreService;

    // In-memory storage for uploaded file metadata (for display purposes)
    private final List<FileInfo> uploadedFiles = new CopyOnWriteArrayList<>();
    private String fileSearchStoreId;

    public GeminiCorpusService(FileSearchStoreService fileSearchStoreService) {
        this.fileSearchStoreService = fileSearchStoreService;
        this.webClient = WebClient.builder()
                .baseUrl(GEMINI_API_BASE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Initialize File Search Store on application startup.
     */
    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("GEMINI_API_KEY not configured. File operations will not work.");
            return;
        }

        try {
            // Create a fresh File Search Store
            createFileSearchStore();
            logger.info("Gemini File Search Store initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Gemini File Search Store", e);
        }
    }

    /**
     * Clean up File Search Store on application shutdown.
     */
    @PreDestroy
    public void cleanup() {
        try {
            deleteFileSearchStore();
            logger.info("Gemini File Search Store cleaned up successfully");
        } catch (Exception e) {
            logger.error("Failed to clean up Gemini File Search Store", e);
        }
    }

    /**
     * Creates a new File Search Store for this session.
     */
    private void createFileSearchStore() throws IOException {
        validateApiKey();

        String displayName = "RAG Store - " + UUID.randomUUID().toString().substring(0, 8);
        this.fileSearchStoreId = fileSearchStoreService.createStore(displayName);
        this.uploadedFiles.clear();

        logger.info("Created new File Search Store: {}", fileSearchStoreId);
    }

    /**
     * Deletes the current File Search Store.
     */
    private void deleteFileSearchStore() {
        if (fileSearchStoreId != null) {
            try {
                fileSearchStoreService.deleteStore(fileSearchStoreId);
                uploadedFiles.clear();
                logger.info("Deleted File Search Store: {}", fileSearchStoreId);
                fileSearchStoreId = null;
            } catch (Exception e) {
                logger.warn("Failed to delete File Search Store during cleanup", e);
                fileSearchStoreId = null;
            }
        }
    }

    /**
     * Uploads a file to the File Search Store.
     * The file is automatically indexed and embedded for semantic search.
     *
     * @param file The file to upload
     * @return FileInfo representing the uploaded file
     * @throws IOException if upload fails
     */
    public FileInfo uploadFile(MultipartFile file) throws IOException {
        validateApiKey();

        if (fileSearchStoreId == null) {
            throw new IOException("File Search Store not initialized. Please restart the application.");
        }

        try {
            String filename = file.getOriginalFilename();

            // Import file to File Search Store
            FileSearchStoreService.DocumentInfo docInfo =
                fileSearchStoreService.importFileToStore(fileSearchStoreId, file);

            // Extract document ID from the full name (format: "stores/xyz/documents/abc")
            String documentId = extractDocumentId(docInfo.getName());

            // Create file info for local tracking (display purposes)
            String fileId = UUID.randomUUID().toString();
            FileInfo fileInfo = new FileInfo(fileId, filename, docInfo.getMimeType(), file.getSize());
            uploadedFiles.add(fileInfo);

            logger.info("File uploaded to File Search Store: {} ({})", filename, documentId);
            return fileInfo;

        } catch (Exception e) {
            logger.error("Failed to upload file to File Search Store: {}", file.getOriginalFilename(), e);
            throw new IOException("Failed to upload file to File Search Store: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a file from the File Search Store.
     *
     * @param fileId The ID of the file to delete
     * @throws IOException if deletion fails
     */
    public void deleteFile(String fileId) throws IOException {
        validateApiKey();

        if (fileSearchStoreId == null) {
            throw new IOException("File Search Store not initialized.");
        }

        try {
            // Find the file by ID and construct the document resource name
            FileInfo fileToDelete = uploadedFiles.stream()
                    .filter(f -> f.getId().equals(fileId))
                    .findFirst()
                    .orElse(null);

            if (fileToDelete == null) {
                logger.warn("File not found for deletion: {}", fileId);
                uploadedFiles.removeIf(f -> f.getId().equals(fileId));
                return;
            }

            // List documents to find matching one
            List<FileSearchStoreService.DocumentInfo> documents =
                fileSearchStoreService.listDocuments(fileSearchStoreId);

            for (FileSearchStoreService.DocumentInfo doc : documents) {
                if (doc.getDisplayName().equals(fileToDelete.getName())) {
                    // Delete from File Search Store
                    fileSearchStoreService.deleteDocument(fileSearchStoreId, extractDocumentId(doc.getName()));
                    logger.info("File deleted from File Search Store: {}", fileId);
                    break;
                }
            }

            // Remove from in-memory list
            uploadedFiles.removeIf(f -> f.getId().equals(fileId));

        } catch (Exception e) {
            logger.error("Failed to delete file from File Search Store: {}", fileId, e);
            uploadedFiles.removeIf(f -> f.getId().equals(fileId));
            throw new IOException("Failed to delete file from File Search Store: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all files in the current File Search Store.
     *
     * @return List of FileInfo
     */
    public List<FileInfo> listFiles() {
        try {
            if (fileSearchStoreId == null) {
                return new ArrayList<>(uploadedFiles);
            }

            // Sync in-memory list with actual store contents
            List<FileSearchStoreService.DocumentInfo> storeDocuments =
                fileSearchStoreService.listDocuments(fileSearchStoreId);

            // Update in-memory list
            uploadedFiles.clear();
            for (FileSearchStoreService.DocumentInfo doc : storeDocuments) {
                String fileId = UUID.randomUUID().toString();
                FileInfo fileInfo = new FileInfo(fileId, doc.getDisplayName(), doc.getMimeType(), doc.getSizeBytes());
                uploadedFiles.add(fileInfo);
            }

            return new ArrayList<>(uploadedFiles);

        } catch (Exception e) {
            logger.warn("Failed to list files from File Search Store, returning in-memory list: {}", e.getMessage());
            return new ArrayList<>(uploadedFiles);
        }
    }

    /**
     * Performs a search query using Gemini 2.5 with File Search.
     * Uses the File Search tool to ground responses in uploaded documents.
     *
     * @param query The search query
     * @param systemPrompt The system prompt to use
     * @return SearchResult containing the response and citations
     * @throws IOException if search fails
     */
    public SearchResult search(String query, String systemPrompt) throws IOException {
        validateApiKey();

        if (fileSearchStoreId == null) {
            throw new IOException("File Search Store not initialized.");
        }

        if (uploadedFiles.isEmpty()) {
            throw new IOException("No files uploaded. Please upload files before searching.");
        }

        try {
            // Build the request for generateContent with File Search tool
            ObjectNode requestBody = objectMapper.createObjectNode();

            // System instruction
            ObjectNode systemInstruction = requestBody.putObject("systemInstruction");
            ArrayNode systemParts = systemInstruction.putArray("parts");
            systemParts.addObject().put("text", systemPrompt);

            // Contents (user query)
            ArrayNode contents = requestBody.putArray("contents");
            ObjectNode userContent = contents.addObject();
            userContent.put("role", "user");
            ArrayNode parts = userContent.putArray("parts");
            parts.addObject().put("text", query);

            // File Search tool configuration
            ArrayNode tools = requestBody.putArray("tools");
            ObjectNode fileSearchTool = tools.addObject();
            ObjectNode fileSearch = fileSearchTool.putObject("fileSearch");
            fileSearch.put("storeUri", fileSearchStoreId);

            // Generation config
            ObjectNode generationConfig = requestBody.putObject("generationConfig");
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 8192);

            String response = webClient.post()
                    .uri("/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseSearchResponse(query, response);

        } catch (Exception e) {
            logger.error("Search failed for query: {}", query, e);
            throw new IOException("Search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the Gemini API response into a SearchResult.
     * Extracts generated text and citations from the response.
     */
    private SearchResult parseSearchResponse(String query, String response) throws IOException {
        JsonNode responseJson = objectMapper.readTree(response);

        // Extract the generated text
        String generatedText = responseJson
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText("");

        // Extract citations if present
        List<String> citations = new ArrayList<>();
        JsonNode citationMetadata = responseJson
                .path("candidates").path(0)
                .path("citationMetadata").path("citationSources");

        if (citationMetadata.isArray()) {
            for (JsonNode citation : citationMetadata) {
                String uri = citation.path("uri").asText();
                String title = citation.path("title").asText();

                // Prefer title over URI for display
                if (!title.isEmpty()) {
                    if (!citations.contains(title)) {
                        citations.add(title);
                    }
                } else if (!uri.isEmpty()) {
                    if (!citations.contains(uri)) {
                        citations.add(uri);
                    }
                }
            }
        }

        // If no API citations, add uploaded file names as sources
        if (citations.isEmpty() && !uploadedFiles.isEmpty()) {
            for (FileInfo file : uploadedFiles) {
                citations.add(file.getName());
            }
        }

        return new SearchResult(query, generatedText, citations);
    }

    /**
     * Extracts the document ID from a full document resource name.
     * E.g., "stores/xyz123/documents/abc456" -> "abc456"
     */
    private String extractDocumentId(String fullDocumentName) {
        if (fullDocumentName == null || fullDocumentName.isEmpty()) {
            return "";
        }
        int lastSlash = fullDocumentName.lastIndexOf('/');
        if (lastSlash >= 0) {
            return fullDocumentName.substring(lastSlash + 1);
        }
        return fullDocumentName;
    }

    /**
     * Uploads text content as a file to the File Search Store.
     *
     * @param textContent The text content to upload
     * @return FileInfo representing the uploaded text file
     * @throws IOException if upload fails
     */
    public FileInfo uploadTextAsFile(String textContent) throws IOException {
        validateApiKey();

        if (fileSearchStoreId == null) {
            throw new IOException("File Search Store not initialized.");
        }

        try {
            String filename = "project-summary-" + System.currentTimeMillis() + ".txt";
            byte[] fileContent = textContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // Create a temporary MultipartFile-like structure
            // Since we can't directly create MultipartFile, we'll use a simpler approach
            // by implementing a minimal MultipartFile wrapper

            org.springframework.web.multipart.MultipartFile textFile =
                new TextMultipartFile(filename, fileContent, "text/plain");

            return uploadFile(textFile);

        } catch (Exception e) {
            logger.error("Failed to upload text as file to File Search Store", e);
            throw new IOException("Failed to upload text file to File Search Store: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that the API key is configured.
     */
    private void validateApiKey() throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Gemini API key not configured. Set GEMINI_API_KEY environment variable.");
        }
    }

    /**
     * Checks if the API key is configured.
     *
     * @return true if API key is available
     */
    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * Helper class to wrap text content as a MultipartFile.
     */
    private static class TextMultipartFile implements MultipartFile {
        private final String filename;
        private final byte[] content;
        private final String mimeType;

        public TextMultipartFile(String filename, byte[] content, String mimeType) {
            this.filename = filename;
            this.content = content;
            this.mimeType = mimeType;
        }

        @Override
        public String getName() {
            return filename;
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return mimeType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content == null ? 0 : content.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            try (var stream = new java.io.FileOutputStream(dest)) {
                stream.write(content);
            }
        }

        @Override
        public void transferTo(java.nio.file.Path dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest, content);
        }
    }
}
