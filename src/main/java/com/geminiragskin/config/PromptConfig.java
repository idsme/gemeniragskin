package com.geminiragskin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration class for prompts used in the RAG system.
 * Values are loaded from application.properties and can be updated at runtime.
 */
@Component
@ConfigurationProperties(prefix = "prompt")
public class PromptConfig {

    private String system;
    private Architecture architecture = new Architecture();

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public void setArchitecture(Architecture architecture) {
        this.architecture = architecture;
    }

    public static class Architecture {
        private String one;
        private String two;
        private String three;

        public String getOne() {
            return one;
        }

        public void setOne(String one) {
            this.one = one;
        }

        public String getTwo() {
            return two;
        }

        public void setTwo(String two) {
            this.two = two;
        }

        public String getThree() {
            return three;
        }

        public void setThree(String three) {
            this.three = three;
        }
    }
}
