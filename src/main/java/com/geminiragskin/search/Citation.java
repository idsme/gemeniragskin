package com.geminiragskin.search;

/**
 * Represents a citation in a search result.
 * Contains detailed information about which document/source grounded the response.
 */
public class Citation {
    private final String uri;              // Document resource URI (stores/xyz/documents/abc)
    private final String title;            // Document display name
    private final int startIndex;          // Character offset start in source
    private final int endIndex;            // Character offset end in source
    private final String excerpt;          // Optional excerpt from the source

    public Citation(String uri, String title, int startIndex, int endIndex) {
        this(uri, title, startIndex, endIndex, null);
    }

    public Citation(String uri, String title, int startIndex, int endIndex, String excerpt) {
        this.uri = uri;
        this.title = title != null && !title.isEmpty() ? title : extractDocumentName(uri);
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.excerpt = excerpt;
    }

    /**
     * Extracts the document name from the full URI.
     * E.g., "stores/xyz/documents/abc123" -> "abc123"
     */
    private static String extractDocumentName(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "Unknown";
        }
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < uri.length() - 1) {
            return uri.substring(lastSlash + 1);
        }
        return uri;
    }

    public String getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public boolean hasExcerpt() {
        return excerpt != null && !excerpt.isEmpty();
    }

    /**
     * Returns character offset information for display.
     * Format: "(lines 145-287)"
     */
    public String getOffsetInfo() {
        if (startIndex < 0 || endIndex < 0) {
            return "";
        }
        return String.format("(chars %d-%d)", startIndex, endIndex);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        if (!getOffsetInfo().isEmpty()) {
            sb.append(" ").append(getOffsetInfo());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Citation)) {
            return false;
        }
        Citation other = (Citation) obj;
        return uri != null && uri.equals(other.uri) &&
               title != null && title.equals(other.title);
    }

    @Override
    public int hashCode() {
        return (uri + "|" + title).hashCode();
    }
}
