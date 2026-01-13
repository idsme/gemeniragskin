package com.geminiragskin.file;

/**
 * DTO representing file metadata for display in the UI.
 */
public class FileInfo {

    private String id;
    private String name;
    private String mimeType;
    private long sizeBytes;
    private String displaySize;
    private String iconClass;

    public FileInfo() {
    }

    public FileInfo(String id, String name, String mimeType, long sizeBytes) {
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.displaySize = formatSize(sizeBytes);
        this.iconClass = determineIconClass(name, mimeType);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    private String determineIconClass(String name, String mimeType) {
        String lowerName = name.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return "icon-pdf";
        } else if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            return "icon-word";
        } else if (lowerName.endsWith(".md")) {
            return "icon-markdown";
        } else if (lowerName.endsWith(".txt")) {
            return "icon-text";
        }
        return "icon-document";
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
        this.displaySize = formatSize(sizeBytes);
    }

    public String getDisplaySize() {
        return displaySize;
    }

    public void setDisplaySize(String displaySize) {
        this.displaySize = displaySize;
    }

    public String getIconClass() {
        return iconClass;
    }

    public void setIconClass(String iconClass) {
        this.iconClass = iconClass;
    }
}
