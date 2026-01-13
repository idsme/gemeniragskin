package com.geminiragskin.search;

import com.geminiragskin.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling search operations.
 */
@Controller
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;
    private final FileService fileService;

    public SearchController(SearchService searchService, FileService fileService) {
        this.searchService = searchService;
        this.fileService = fileService;
    }

    /**
     * Handles search requests.
     *
     * @param query The search query
     * @param redirectAttributes For adding flash messages
     * @return Redirect to main page
     */
    @PostMapping("/search")
    public String search(
            @RequestParam("query") String query,
            RedirectAttributes redirectAttributes) {

        // Check if files exist
        if (!fileService.hasFiles()) {
            redirectAttributes.addFlashAttribute("searchError",
                    "Please upload files first before searching");
            return "redirect:/";
        }

        // Validate query
        if (query == null || query.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("searchError",
                    "Please enter a search query");
            return "redirect:/";
        }

        try {
            searchService.search(query.trim());
            logger.info("Search executed successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid search query: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("searchError", e.getMessage());
        } catch (Exception e) {
            logger.error("Search failed", e);
            redirectAttributes.addFlashAttribute("searchError",
                    "Search failed: " + e.getMessage() + ". Please try again.");
        }

        return "redirect:/";
    }
}
