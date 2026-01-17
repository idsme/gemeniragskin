package com.geminiragskin.search;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a search result from the Gemini API.
 * Supports both simple string citations (for backwards compatibility) and
 * detailed Citation objects with character offsets and excerpts.
 */
public class SearchResult {

    private String query;
    private String response;
    private String responseHtml; // Pre-rendered markdown
    private List<String> citations;  // For backwards compatibility
    private List<Citation> detailedCitations;  // Enhanced citation data
    private LocalDateTime timestamp;
    private String formattedTimestamp;

    public SearchResult() {
        this.citations = new ArrayList<>();
        this.detailedCitations = new ArrayList<>();
        this.timestamp = LocalDateTime.now();
        this.formattedTimestamp = formatTimestamp(this.timestamp);
    }

    public SearchResult(String query, String response, List<String> citations) {
        this.query = query;
        this.response = response;
        this.citations = citations != null ? citations : new ArrayList<>();
        this.detailedCitations = new ArrayList<>();
        this.timestamp = LocalDateTime.now();
        this.formattedTimestamp = formatTimestamp(this.timestamp);
    }

    public SearchResult(String query, String response, List<String> citations, List<Citation> detailedCitations) {
        this.query = query;
        this.response = response;
        this.citations = citations != null ? citations : new ArrayList<>();
        this.detailedCitations = detailedCitations != null ? detailedCitations : new ArrayList<>();
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

    public List<Citation> getDetailedCitations() {
        return detailedCitations;
    }

    public void setDetailedCitations(List<Citation> detailedCitations) {
        this.detailedCitations = detailedCitations != null ? detailedCitations : new ArrayList<>();
    }

    public void addDetailedCitation(Citation citation) {
        if (this.detailedCitations == null) {
            this.detailedCitations = new ArrayList<>();
        }
        this.detailedCitations.add(citation);
    }

    public boolean hasDetailedCitations() {
        return detailedCitations != null && !detailedCitations.isEmpty();
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
                ", detailedCitations=" + detailedCitations +
                ", timestamp=" + timestamp +
                ", formattedTimestamp='" + formattedTimestamp + '\'' +
                '}';
    }
}
