package com.geminiragskin.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for the application.
 * Provides user-friendly error messages for various exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles file size exceeded exceptions.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException e,
                                         RedirectAttributes redirectAttributes) {
        logger.warn("File size exceeded: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("uploadError",
                "File size exceeds the maximum limit of 50MB. Please upload a smaller file.");
        return "redirect:/";
    }

    /**
     * Handles generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception e,
                                         RedirectAttributes redirectAttributes) {
        logger.error("Unexpected error occurred", e);
        redirectAttributes.addFlashAttribute("error",
                "An unexpected error occurred. Please try again.");
        return "redirect:/";
    }
}
