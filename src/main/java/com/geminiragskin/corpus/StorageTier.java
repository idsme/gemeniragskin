package com.geminiragskin.corpus;

/**
 * Enum representing Gemini File Search storage tiers.
 * Each tier has specific storage capacity and pricing implications.
 */
public enum StorageTier {
    /**
     * Free tier: 1 GB storage
     * Suitable for development and small projects
     */
    FREE("free", 1L * 1024 * 1024 * 1024, "Free (1 GB)"),

    /**
     * Tier 1: 10 GB storage
     * Suitable for small to medium projects
     */
    TIER_1("tier1", 10L * 1024 * 1024 * 1024, "Tier 1 (10 GB)"),

    /**
     * Tier 2: 100 GB storage
     * Suitable for medium to large projects
     */
    TIER_2("tier2", 100L * 1024 * 1024 * 1024, "Tier 2 (100 GB)"),

    /**
     * Tier 3: 1 TB storage
     * Suitable for large enterprise projects
     */
    TIER_3("tier3", 1024L * 1024 * 1024 * 1024, "Tier 3 (1 TB)");

    private final String identifier;
    private final long maxBytes;
    private final String displayName;

    StorageTier(String identifier, long maxBytes, String displayName) {
        this.identifier = identifier;
        this.maxBytes = maxBytes;
        this.displayName = displayName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the maximum storage in GB.
     */
    public long getMaxGB() {
        return maxBytes / (1024 * 1024 * 1024);
    }

    /**
     * Parses a storage tier from a string identifier.
     *
     * @param identifier The tier identifier (e.g., "free", "tier1", "tier2", "tier3")
     * @return The matching StorageTier, or FREE if not found
     */
    public static StorageTier fromString(String identifier) {
        if (identifier == null) {
            return FREE;
        }
        String lower = identifier.toLowerCase().trim();
        for (StorageTier tier : StorageTier.values()) {
            if (tier.identifier.equals(lower)) {
                return tier;
            }
        }
        return FREE;  // Default to free tier
    }

    /**
     * Gets the recommended tier based on total file size.
     *
     * @param totalSizeBytes Total size of files in bytes
     * @return Recommended StorageTier
     */
    public static StorageTier recommendedTier(long totalSizeBytes) {
        long sizeGB = totalSizeBytes / (1024 * 1024 * 1024);

        if (sizeGB <= 1) {
            return FREE;
        } else if (sizeGB <= 10) {
            return TIER_1;
        } else if (sizeGB <= 100) {
            return TIER_2;
        } else {
            return TIER_3;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}
