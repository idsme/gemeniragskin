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
     * Handles saving prompt configuration with support for dynamic number of prompts.
     *
     * @param systemPrompt The system prompt
     * @param allParams All request parameters (to capture dynamic prompt fields)
     * @param redirectAttributes For adding flash messages
     * @return Redirect to main page
     */
    @PostMapping("/config/save")
    public String saveConfig(
            @RequestParam("systemPrompt") String systemPrompt,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes) {

        try {
            // Extract all architecture prompts (prompt1, prompt2, prompt3, ...)
            List<String> architecturePrompts = new ArrayList<>();
            int i = 1;
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

            promptConfigService.savePrompts(systemPrompt, architecturePrompts);
            redirectAttributes.addFlashAttribute("configSuccess",
                "Configuration saved successfully! (" + architecturePrompts.size() + " prompts)");
            logger.info("Configuration saved successfully with {} architecture prompts", architecturePrompts.size());
        } catch (Exception e) {
            logger.error("Failed to save configuration", e);
            redirectAttributes.addFlashAttribute("configError", "Failed to save configuration: " + e.getMessage());
        }

        return "redirect:/";
    }
}
