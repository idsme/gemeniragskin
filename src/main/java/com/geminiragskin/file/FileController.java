package com.geminiragskin.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for handling file upload and delete operations.
 */
@Controller
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Handles multi-file upload.
     *
     * @param files The files to upload
     * @param redirectAttributes For adding flash messages
     * @return Redirect to main page
     */
    @PostMapping("/upload")
    public String uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            RedirectAttributes redirectAttributes) {

        if (files == null || files.length == 0) {
            redirectAttributes.addFlashAttribute("uploadError", "No files selected for upload");
            return "redirect:/";
        }

        try {
            List<FileInfo> uploadedFiles = fileService.uploadFiles(files);
            int count = uploadedFiles.size();
            String message = count == 1
                    ? "File uploaded successfully: " + uploadedFiles.get(0).getName()
                    : count + " files uploaded successfully";
            redirectAttributes.addFlashAttribute("uploadSuccess", message);
            logger.info("Uploaded {} file(s)", count);
        } catch (FileValidationException e) {
            logger.warn("File validation failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
        } catch (Exception e) {
            logger.error("File upload failed", e);
            redirectAttributes.addFlashAttribute("uploadError",
                    "Upload failed: " + e.getMessage() + ". Please try again.");
        }

        return "redirect:/";
    }

    /**
     * Handles file deletion.
     *
     * @param fileId The ID of the file to delete
     * @param redirectAttributes For adding flash messages
     * @return Redirect to main page
     */
    @PostMapping("/delete/{fileId}")
    public String deleteFile(
            @PathVariable("fileId") String fileId,
            RedirectAttributes redirectAttributes) {

        try {
            fileService.deleteFile(fileId);
            redirectAttributes.addFlashAttribute("deleteSuccess", "File deleted successfully");
            logger.info("Deleted file: {}", fileId);
        } catch (Exception e) {
            logger.error("File deletion failed for file: {}", fileId, e);
            redirectAttributes.addFlashAttribute("deleteError",
                    "Failed to delete file: " + e.getMessage());
        }

        return "redirect:/";
    }
}
