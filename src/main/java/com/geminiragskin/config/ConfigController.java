package com.geminiragskin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling prompt configuration requests.
 */
@Controller
public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    private final PromptConfigService promptConfigService;

    public ConfigController(PromptConfigService promptConfigService) {
        this.promptConfigService = promptConfigService;
    }

    /**
     * Handles saving prompt configuration.
     *
     * @param systemPrompt The system prompt
     * @param prompt1      First architecture prompt
     * @param prompt2      Second architecture prompt
     * @param prompt3      Third architecture prompt
     * @param redirectAttributes For adding flash messages
     * @return Redirect to main page
     */
    @PostMapping("/config/save")
    public String saveConfig(
            @RequestParam("systemPrompt") String systemPrompt,
            @RequestParam("prompt1") String prompt1,
            @RequestParam("prompt2") String prompt2,
            @RequestParam("prompt3") String prompt3,
            RedirectAttributes redirectAttributes) {

        try {
            promptConfigService.savePrompts(systemPrompt, prompt1, prompt2, prompt3);
            redirectAttributes.addFlashAttribute("configSuccess", "Configuration saved successfully!");
            logger.info("Configuration saved successfully");
        } catch (Exception e) {
            logger.error("Failed to save configuration", e);
            redirectAttributes.addFlashAttribute("configError", "Failed to save configuration: " + e.getMessage());
        }

        return "redirect:/";
    }
}
