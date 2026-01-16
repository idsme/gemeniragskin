package com.geminiragskin.web;

import com.geminiragskin.config.PromptConfigService;
import com.geminiragskin.corpus.GeminiCorpusService;
import com.geminiragskin.file.FileService;
import com.geminiragskin.search.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Main controller for rendering the index page with all required data.
 */
@Controller
public class MainController {

    private final PromptConfigService promptConfigService;
    private final FileService fileService;
    private final SearchService searchService;
    private final GeminiCorpusService geminiCorpusService;

    public MainController(PromptConfigService promptConfigService,
                          FileService fileService,
                          SearchService searchService,
                          GeminiCorpusService geminiCorpusService) {
        this.promptConfigService = promptConfigService;
        this.fileService = fileService;
        this.searchService = searchService;
        this.geminiCorpusService = geminiCorpusService;
    }

    /**
     * Renders the main index page with all data.
     *
     * @param model The model to populate with data
     * @return The index template name
     */
    @GetMapping("/")
    public String index(Model model) {
        // Config section data - system prompts
        model.addAttribute("systemPrompt", promptConfigService.getSystemPrompt());
        model.addAttribute("allSystemPrompts", promptConfigService.getAllSystemPrompts());
        model.addAttribute("selectedSystemPromptIndex", promptConfigService.getSelectedSystemPromptIndex());

        // Config section data - architecture prompts
        model.addAttribute("prompt1", promptConfigService.getPrompt1());
        model.addAttribute("prompt2", promptConfigService.getPrompt2());
        model.addAttribute("prompt3", promptConfigService.getPrompt3());

        // Add all prompts as a list for dynamic rendering
        model.addAttribute("allPrompts", promptConfigService.getAllPrompts());

        // File list data
        model.addAttribute("files", fileService.listFiles());
        model.addAttribute("hasFiles", fileService.hasFiles());

        // Search results data
        model.addAttribute("searchResults", searchService.getSearchHistory());
        model.addAttribute("hasSearchResults", searchService.hasSearchHistory());
        model.addAttribute("lastQuery", searchService.getLastQuery());

        // API status
        model.addAttribute("apiConfigured", geminiCorpusService.isApiKeyConfigured());

        return "index";
    }
}
