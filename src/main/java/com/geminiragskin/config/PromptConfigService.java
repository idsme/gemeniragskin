package com.geminiragskin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Service for managing prompt configuration.
 * Handles reading and writing prompts to application.properties file.
 */
@Service
public class PromptConfigService {

    private static final Logger logger = LoggerFactory.getLogger(PromptConfigService.class);

    @Value("${prompt.system}")
    private String systemPrompt;

    @Value("${prompt.architecture.1}")
    private String prompt1;

    @Value("${prompt.architecture.2}")
    private String prompt2;

    @Value("${prompt.architecture.3}")
    private String prompt3;

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getPrompt1() {
        return prompt1;
    }

    public String getPrompt2() {
        return prompt2;
    }

    public String getPrompt3() {
        return prompt3;
    }

    /**
     * Saves prompt configuration to the application.properties file.
     *
     * @param system  The system prompt
     * @param prompt1 First architecture prompt
     * @param prompt2 Second architecture prompt
     * @param prompt3 Third architecture prompt
     * @throws IOException if there's an error writing to the file
     */
    public void savePrompts(String system, String prompt1, String prompt2, String prompt3) throws IOException {
        // Update in-memory values
        this.systemPrompt = system;
        this.prompt1 = prompt1;
        this.prompt2 = prompt2;
        this.prompt3 = prompt3;

        // Find the application.properties file
        Path propertiesPath = findPropertiesFile();

        if (propertiesPath != null && Files.exists(propertiesPath)) {
            Properties props = new Properties();

            // Read existing properties
            try (InputStream is = Files.newInputStream(propertiesPath)) {
                props.load(is);
            }

            // Update prompt properties
            props.setProperty("prompt.system", system);
            props.setProperty("prompt.architecture.1", prompt1);
            props.setProperty("prompt.architecture.2", prompt2);
            props.setProperty("prompt.architecture.3", prompt3);

            // Write back to file
            try (OutputStream os = Files.newOutputStream(propertiesPath)) {
                props.store(os, "Gemini RAG Skin Application Configuration");
            }

            logger.info("Prompts saved successfully to {}", propertiesPath);
        } else {
            logger.warn("Could not find application.properties file for persistent storage");
            // Values are still updated in memory for the current session
        }
    }

    private Path findPropertiesFile() {
        // Try to find the properties file in common locations
        String[] paths = {
            "src/main/resources/application.properties",
            "application.properties",
            "config/application.properties"
        };

        for (String path : paths) {
            Path p = Path.of(path);
            if (Files.exists(p)) {
                return p;
            }
        }

        // Try to find from classpath resource
        try {
            ClassPathResource resource = new ClassPathResource("application.properties");
            if (resource.exists()) {
                return Path.of(resource.getFile().getAbsolutePath());
            }
        } catch (IOException e) {
            logger.debug("Could not find application.properties from classpath", e);
        }

        return null;
    }
}
