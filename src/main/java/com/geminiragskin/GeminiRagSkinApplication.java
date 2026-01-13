package com.geminiragskin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Gemini RAG Skin.
 * A lightweight RAG tool for querying project documentation using Google's Gemini File Search API.
 */
@SpringBootApplication
public class GeminiRagSkinApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeminiRagSkinApplication.class, args);
    }
}
