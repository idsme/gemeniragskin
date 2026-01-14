package com.geminiragskin.search;

import com.geminiragskin.config.PromptConfigService;
import com.geminiragskin.corpus.GeminiCorpusService;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Service for handling search operations using the Gemini API.
 */
@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final GeminiCorpusService geminiCorpusService;
    private final PromptConfigService promptConfigService;
    private final QueryHistory queryHistory;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    public SearchService(GeminiCorpusService geminiCorpusService,
                         PromptConfigService promptConfigService,
                         QueryHistory queryHistory) {
        this.geminiCorpusService = geminiCorpusService;
        this.promptConfigService = promptConfigService;
        this.queryHistory = queryHistory;
        this.markdownParser = Parser.builder().build();
        this.htmlRenderer = HtmlRenderer.builder().build();
    }

    /**
     * Performs a search query against the uploaded documents.
     * Uses the currently selected system prompt.
     *
     * @param query The user's search query
     * @return SearchResult containing the response and citations
     * @throws IOException if there's an error querying the API
     */
    public SearchResult search(String query) throws IOException {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        // Use the active (selected) system prompt
        String systemPrompt = promptConfigService.getActiveSystemPrompt();
        SearchResult result = geminiCorpusService.search(query, systemPrompt);

        // Render markdown to HTML
        String responseHtml = renderMarkdown(result.getResponse());
        result.setResponseHtml(responseHtml);

        // Add to history
        queryHistory.addResult(result);

        logger.info("Search completed for query: {} using system prompt index: {}",
            query.substring(0, Math.min(50, query.length())),
            promptConfigService.getSelectedSystemPromptIndex());
        return result;
    }

    /**
     * Gets all search results from the current session.
     *
     * @return List of search results (most recent first)
     */
    public List<SearchResult> getSearchHistory() {
        return queryHistory.getResults();
    }

    /**
     * Checks if there are any results in the history.
     *
     * @return true if history has results
     */
    public boolean hasSearchHistory() {
        return queryHistory.hasResults();
    }

    /**
     * Clears the search history for the current session.
     */
    public void clearHistory() {
        queryHistory.clear();
    }

    private String renderMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        Node document = markdownParser.parse(markdown);
        return htmlRenderer.render(document);
    }
}
