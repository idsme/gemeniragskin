package com.geminiragskin.file;

import com.geminiragskin.corpus.GeminiCorpusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Service for handling file operations including validation and Gemini corpus management.
 */
@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB (Gemini 2.5 supports up to 100MB)

    // Expanded to support all formats that Gemini 2.5 File Search handles
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // Documents
            ".pdf", ".doc", ".docx", ".odt", ".rtf",
            // Spreadsheets
            ".csv", ".xlsx", ".xls", ".ods",
            // Presentations
            ".pptx", ".ppt", ".odp",
            // Text files
            ".txt", ".md", ".html", ".htm", ".xml", ".json", ".yaml", ".yml", ".toml",
            // Code files
            ".py", ".js", ".ts", ".jsx", ".tsx", ".java", ".cpp", ".c", ".h", ".hpp",
            ".cs", ".go", ".rb", ".rs", ".php", ".swift", ".kt", ".scala", ".r", ".m",
            // Other formats
            ".log", ".sql", ".sh", ".bash", ".groovy", ".gradle"
    );

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            // Documents
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.oasis.opendocument.text",
            "application/rtf",
            // Spreadsheets
            "text/csv",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/vnd.oasis.opendocument.spreadsheet",
            // Presentations
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint",
            "application/vnd.oasis.opendocument.presentation",
            // Text
            "text/plain",
            "text/markdown",
            "text/html",
            "application/xml",
            "text/xml",
            "application/json",
            "text/yaml",
            // Code
            "text/x-python",
            "text/javascript",
            "text/typescript",
            "text/x-java",
            "text/x-c",
            "text/x-c++",
            "text/x-csharp",
            "text/x-go",
            "text/x-ruby",
            "text/x-rust",
            "text/x-php",
            "text/x-swift",
            "text/x-kotlin",
            "text/x-scala",
            "text/x-r",
            "text/x-objectivec",
            "text/x-sh",
            "text/x-sql",
            "text/x-log"
    );

    private final GeminiCorpusService geminiCorpusService;

    public FileService(GeminiCorpusService geminiCorpusService) {
        this.geminiCorpusService = geminiCorpusService;
    }

    /**
     * Validates and uploads a file to the Gemini corpus.
     *
     * @param file The file to upload
     * @return The FileInfo representing the uploaded file
     * @throws FileValidationException if the file fails validation
     * @throws IOException if there's an error during upload
     */
    public FileInfo uploadFile(MultipartFile file) throws FileValidationException, IOException {
        validateFile(file);
        return geminiCorpusService.uploadFile(file);
    }

    /**
     * Uploads multiple files to the Gemini corpus.
     *
     * @param files The files to upload
     * @return List of FileInfo representing uploaded files
     * @throws FileValidationException if any file fails validation
     * @throws IOException if there's an error during upload
     */
    public List<FileInfo> uploadFiles(MultipartFile[] files) throws FileValidationException, IOException {
        // Validate all files first
        for (MultipartFile file : files) {
            validateFile(file);
        }

        // Upload all files
        return Arrays.stream(files)
                .map(file -> {
                    try {
                        return geminiCorpusService.uploadFile(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
                    }
                })
                .toList();
    }

    /**
     * Deletes a file from the Gemini corpus.
     *
     * @param fileId The ID of the file to delete
     * @throws IOException if there's an error during deletion
     */
    public void deleteFile(String fileId) throws IOException {
        geminiCorpusService.deleteFile(fileId);
    }

    /**
     * Uploads text content as a file to the Gemini corpus.
     *
     * @param textContent The text content to upload
     * @return The FileInfo representing the uploaded text file
     * @throws IOException if there's an error during upload
     */
    public FileInfo uploadTextAsFile(String textContent) throws IOException {
        if (textContent == null || textContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Text content cannot be empty");
        }

        return geminiCorpusService.uploadTextAsFile(textContent);
    }

    /**
     * Lists all files in the current corpus.
     *
     * @return List of FileInfo representing all files
     */
    public List<FileInfo> listFiles() {
        return geminiCorpusService.listFiles();
    }

    /**
     * Checks if any files have been uploaded.
     *
     * @return true if files exist in the corpus
     */
    public boolean hasFiles() {
        return !geminiCorpusService.listFiles().isEmpty();
    }

    private void validateFile(MultipartFile file) throws FileValidationException {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File is empty or not provided");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new FileValidationException("File name is required");
        }

        // Check file size (Gemini 2.5 supports up to 100MB per file)
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException(
                    String.format("File size exceeds the maximum limit of 100MB. File size: %.1f MB",
                            file.getSize() / (1024.0 * 1024.0)));
        }

        // Check file extension
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileValidationException(
                    String.format("Invalid file type '%s'. Allowed types: PDF, Word (doc/docx), Excel (xlsx/xls), " +
                            "PowerPoint (pptx/ppt), Text (txt/md/html/json/xml), Code (py/js/java/cpp/cs/go/rb), and others",
                            extension));
        }

        logger.debug("File validated successfully: {}", originalFilename);
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
}
