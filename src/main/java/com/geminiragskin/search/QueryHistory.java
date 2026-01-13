package com.geminiragskin.search;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Session-scoped component for storing query history.
 * Each user session has its own history.
 */
@Component
@SessionScope
public class QueryHistory {

    private final List<SearchResult> results;

    public QueryHistory() {
        this.results = new ArrayList<>();
    }

    /**
     * Adds a new search result to the history.
     * New results are added at the beginning (most recent first).
     *
     * @param result The search result to add
     */
    public void addResult(SearchResult result) {
        results.add(0, result);
    }

    /**
     * Gets all search results in the history.
     * Results are ordered most recent first.
     *
     * @return Unmodifiable list of search results
     */
    public List<SearchResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    /**
     * Clears all search history.
     */
    public void clear() {
        results.clear();
    }

    /**
     * Checks if there are any results in the history.
     *
     * @return true if history is not empty
     */
    public boolean hasResults() {
        return !results.isEmpty();
    }

    /**
     * Gets the number of results in the history.
     *
     * @return Number of results
     */
    public int size() {
        return results.size();
    }
}
