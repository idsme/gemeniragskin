package com.geminiragskin.search;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a search result from the Gemini API.
 */
public class SearchResult {

    private String query;
    private String response;
    private String responseHtml; // Pre-rendered markdown
    private List<String> citations;
    private LocalDateTime timestamp;
    private String formattedTimestamp;

    public SearchResult() {
        this.citations = new ArrayList<>();
        this.timestamp = LocalDateTime.now();
        this.formattedTimestamp = formatTimestamp(this.timestamp);
    }

    public SearchResult(String query, String response, List<String> citations) {
        this.query = query;
        this.response = response;
        this.citations = citations != null ? citations : new ArrayList<>();
        this.timestamp = LocalDateTime.now();
        this.formattedTimestamp = formatTimestamp(this.timestamp);
    }

    private String formatTimestamp(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
        return dateTime.format(formatter);
    }

    // Getters and setters

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponseHtml() {
        return responseHtml;
    }

    public void setResponseHtml(String responseHtml) {
        this.responseHtml = responseHtml;
    }

    public List<String> getCitations() {
        return citations;
    }

    public void setCitations(List<String> citations) {
        this.citations = citations;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        this.formattedTimestamp = formatTimestamp(timestamp);
    }

    public String getFormattedTimestamp() {
        return formattedTimestamp;
    }

    public void setFormattedTimestamp(String formattedTimestamp) {
        this.formattedTimestamp = formattedTimestamp;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "query='" + query + '\'' +
                ", response='" + response + '\'' +
                ", responseHtml='" + responseHtml + '\'' +
                ", citations=" + citations +
                ", timestamp=" + timestamp +
                ", formattedTimestamp='" + formattedTimestamp + '\'' +
                '}';
    }
}
