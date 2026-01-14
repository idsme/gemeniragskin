package com.geminiragskin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * Handles saving prompt configuration with support for dynamic number of prompts
     * and multiple system prompts.
     *
     * @param allParams All request parameters (to capture dynamic prompt fields)
     * @param redirectAttributes For adding flash messages
     * @return Redirect to main page
     */
    @PostMapping("/config/save")
    public String saveConfig(
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes) {

        try {
            // Extract all system prompts (systemPrompt1, systemPrompt2, ...)
            List<String> systemPrompts = new ArrayList<>();
            int i = 1;
            while (allParams.containsKey("systemPrompt" + i)) {
                String systemPrompt = allParams.get("systemPrompt" + i);
                // Only add non-empty prompts
                if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                    systemPrompts.add(systemPrompt);
                }
                i++;
            }

            // Fallback to legacy single system prompt if no numbered prompts found
            if (systemPrompts.isEmpty() && allParams.containsKey("systemPrompt")) {
                systemPrompts.add(allParams.get("systemPrompt"));
            }

            // Get selected system prompt index
            int selectedIndex = 1; // Default to first prompt
            if (allParams.containsKey("selectedSystemPrompt")) {
                try {
                    selectedIndex = Integer.parseInt(allParams.get("selectedSystemPrompt"));
                } catch (NumberFormatException e) {
                    logger.warn("Invalid selected system prompt index, defaulting to 1");
                }
            }

            // Extract all architecture prompts (prompt1, prompt2, prompt3, ...)
            List<String> architecturePrompts = new ArrayList<>();
            i = 1;
            while (allParams.containsKey("prompt" + i)) {
                String prompt = allParams.get("prompt" + i);
                // Only add non-empty prompts
                if (prompt != null && !prompt.trim().isEmpty()) {
                    architecturePrompts.add(prompt);
                }
                i++;
            }

            // Ensure at least 3 prompts exist (maintain backward compatibility)
            while (architecturePrompts.size() < 3) {
                architecturePrompts.add("");
            }

            // Ensure at least 1 system prompt exists
            if (systemPrompts.isEmpty()) {
                systemPrompts.add("You are a solution architect assistant. Answer questions based on the uploaded project documents.");
            }

            promptConfigService.savePrompts(systemPrompts, selectedIndex, architecturePrompts);
            redirectAttributes.addFlashAttribute("configSuccess",
                "Configuration saved successfully! (" + systemPrompts.size() + " system prompts, " +
                architecturePrompts.size() + " architecture prompts)");
            logger.info("Configuration saved successfully with {} system prompts and {} architecture prompts",
                systemPrompts.size(), architecturePrompts.size());
        } catch (Exception e) {
            logger.error("Failed to save configuration", e);
            redirectAttributes.addFlashAttribute("configError", "Failed to save configuration: " + e.getMessage());
        }

        return "redirect:/";
    }
}
