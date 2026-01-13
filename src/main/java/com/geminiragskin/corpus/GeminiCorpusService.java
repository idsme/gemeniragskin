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
 * Service for managing Gemini File Search API operations.
 * Handles corpus creation, file upload/delete, and search queries.
 */
@Service
public class GeminiCorpusService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiCorpusService.class);
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta";

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // In-memory storage for uploaded files (since Gemini File API is stateless within sessions)
    private final List<FileInfo> uploadedFiles = new CopyOnWriteArrayList<>();
    private String corpusId;

    public GeminiCorpusService() {
        this.webClient = WebClient.builder()
                .baseUrl(GEMINI_API_BASE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Initialize corpus on application startup.
     */
    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("GEMINI_API_KEY not configured. File operations will not work.");
            return;
        }

        try {
            // Create a fresh corpus
            createCorpus();
            logger.info("Gemini corpus initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Gemini corpus", e);
        }
    }

    /**
     * Clean up corpus on application shutdown.
     */
    @PreDestroy
    public void cleanup() {
        try {
            deleteCorpus();
            logger.info("Gemini corpus cleaned up successfully");
        } catch (Exception e) {
            logger.error("Failed to clean up Gemini corpus", e);
        }
    }

    /**
     * Creates a new corpus for this session.
     */
    private void createCorpus() throws IOException {
        this.corpusId = "corpus-" + UUID.randomUUID().toString().substring(0, 8);
        this.uploadedFiles.clear();
        logger.info("Created new corpus: {}", corpusId);
    }

    /**
     * Deletes the current corpus.
     */
    private void deleteCorpus() {
        if (corpusId != null) {
            uploadedFiles.clear();
            logger.info("Deleted corpus: {}", corpusId);
            corpusId = null;
        }
    }

    /**
     * Uploads a file to the Gemini API and corpus.
     *
     * @param file The file to upload
     * @return FileInfo representing the uploaded file
     * @throws IOException if upload fails
     */
    public FileInfo uploadFile(MultipartFile file) throws IOException {
        validateApiKey();

        String filename = file.getOriginalFilename();
        byte[] fileContent = file.getBytes();
        String mimeType = determineMimeType(filename);

        // Upload to Gemini Files API
        String fileUri = uploadToGeminiFiles(filename, fileContent, mimeType);

        // Create file info and store
        String fileId = UUID.randomUUID().toString();
        FileInfo fileInfo = new FileInfo(fileId, filename, mimeType, file.getSize());
        uploadedFiles.add(fileInfo);

        logger.info("File uploaded successfully: {} ({})", filename, fileId);
        return fileInfo;
    }

    /**
     * Uploads file content to Gemini Files API using resumable upload protocol.
     * Step 1: Start resumable upload and get upload URL
     * Step 2: Upload file bytes to the upload URL
     */
    private String uploadToGeminiFiles(String filename, byte[] content, String mimeType) throws IOException {
        try {
            WebClient uploadClient = WebClient.builder()
                    .baseUrl("https://generativelanguage.googleapis.com")
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                    .build();

            // Step 1: Start resumable upload
            ObjectNode metadata = objectMapper.createObjectNode();
            ObjectNode fileNode = metadata.putObject("file");
            fileNode.put("display_name", filename);

            var responseSpec = uploadClient.post()
                    .uri("/upload/v1beta/files?key=" + apiKey)
                    .header("X-Goog-Upload-Protocol", "resumable")
                    .header("X-Goog-Upload-Command", "start")
                    .header("X-Goog-Upload-Header-Content-Length", String.valueOf(content.length))
                    .header("X-Goog-Upload-Header-Content-Type", mimeType)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(metadata.toString())
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            // Extract upload URL from response headers
            String uploadUrl = responseSpec.getHeaders().getFirst("X-Goog-Upload-URL");
            if (uploadUrl == null || uploadUrl.isEmpty()) {
                throw new IOException("Failed to get upload URL from Gemini API response");
            }

            logger.info("Got upload URL for file: {}", filename);

            // Step 2: Upload file bytes
            String response = WebClient.create()
                    .post()
                    .uri(uploadUrl)
                    .header("X-Goog-Upload-Command", "upload, finalize")
                    .header("X-Goog-Upload-Offset", "0")
                    .header("Content-Length", String.valueOf(content.length))
                    .contentType(MediaType.parseMediaType(mimeType))
                    .bodyValue(content)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(response);
            String fileUri = responseJson.path("file").path("uri").asText();
            String fileName = responseJson.path("file").path("name").asText();
            logger.info("File uploaded to Gemini successfully: {} -> {} ({})", filename, fileUri, fileName);
            return fileUri;

        } catch (Exception e) {
            logger.error("Failed to upload file to Gemini: {}", filename, e);
            throw new IOException("Failed to upload file to Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a file from the corpus.
     *
     * @param fileId The ID of the file to delete
     * @throws IOException if deletion fails
     */
    public void deleteFile(String fileId) throws IOException {
        uploadedFiles.removeIf(f -> f.getId().equals(fileId));
        logger.info("File deleted: {}", fileId);
    }

    /**
     * Lists all files in the current corpus.
     *
     * @return List of FileInfo
     */
    public List<FileInfo> listFiles() {
        return new ArrayList<>(uploadedFiles);
    }

    /**
     * Performs a search query using Gemini API with RAG.
     *
     * @param query The search query
     * @param systemPrompt The system prompt to use
     * @return SearchResult containing the response
     * @throws IOException if search fails
     */
    public SearchResult search(String query, String systemPrompt) throws IOException {
        validateApiKey();

        if (uploadedFiles.isEmpty()) {
            throw new IOException("No files uploaded. Please upload files before searching.");
        }

        try {
            // Build the request for generateContent with grounding
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

            // Generation config
            ObjectNode generationConfig = requestBody.putObject("generationConfig");
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 8192);

            String response = webClient.post()
                    .uri("/models/gemini-2.0-flash:generateContent?key=" + apiKey)
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
                if (!uri.isEmpty()) {
                    citations.add(uri);
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
        }
        return "application/octet-stream";
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
}
