package com.geminiragskin.corpus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for managing File Search Store storage capacity and tier upgrades.
 * Monitors storage usage and provides recommendations for tier changes.
 */
@Service
public class StorageManager {

    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    @Value("${gemini.storage.tier:free}")
    private String configuredTier;

    @Value("${gemini.storage.auto-upgrade:true}")
    private boolean autoUpgradeEnabled;

    @Value("${gemini.storage.alert-threshold:80}")
    private int alertThresholdPercent;

    private StorageTier currentTier;
    private long estimatedUsageBytes;

    public StorageManager() {
        this.currentTier = StorageTier.FREE;
        this.estimatedUsageBytes = 0;
    }

    /**
     * Initialize storage tier from configuration.
     */
    public void init() {
        this.currentTier = StorageTier.fromString(configuredTier);
        logger.info("Storage Manager initialized with tier: {} ({})",
            currentTier.getDisplayName(), currentTier.getMaxGB() + " GB");
    }

    /**
     * Updates estimated storage usage.
     *
     * @param totalUsageBytes Total storage used in bytes
     */
    public void updateUsage(long totalUsageBytes) {
        this.estimatedUsageBytes = totalUsageBytes;

        double usagePercent = getUsagePercentage();
        logger.debug("Storage usage: {:.1f}% of {} tier", usagePercent, currentTier.getDisplayName());

        // Check if we should alert
        if (usagePercent >= alertThresholdPercent) {
            logger.warn("Storage usage alert: {:.1f}% of {} tier capacity",
                usagePercent, currentTier.getDisplayName());

            if (autoUpgradeEnabled) {
                StorageTier recommendedTier = StorageTier.recommendedTier(totalUsageBytes);
                if (recommendedTier.getMaxBytes() > currentTier.getMaxBytes()) {
                    logger.info("Recommending tier upgrade from {} to {}",
                        currentTier.getDisplayName(), recommendedTier.getDisplayName());
                }
            }
        }
    }

    /**
     * Adds file size to current usage estimate.
     *
     * @param fileSizeBytes Size of file being added
     */
    public void addFileSize(long fileSizeBytes) {
        this.estimatedUsageBytes += fileSizeBytes;

        if (wouldExceedLimit(fileSizeBytes)) {
            logger.warn("Adding file would approach or exceed storage limit. " +
                "Current: {} / {}", formatBytes(estimatedUsageBytes),
                formatBytes(currentTier.getMaxBytes()));
        }
    }

    /**
     * Removes file size from current usage estimate.
     *
     * @param fileSizeBytes Size of file being removed
     */
    public void removeFileSize(long fileSizeBytes) {
        this.estimatedUsageBytes = Math.max(0, estimatedUsageBytes - fileSizeBytes);
    }

    /**
     * Checks if adding a file would exceed the current tier's limit.
     *
     * @param fileSizeBytes Size of file to check
     * @return true if would exceed limit
     */
    public boolean wouldExceedLimit(long fileSizeBytes) {
        return (estimatedUsageBytes + fileSizeBytes) > currentTier.getMaxBytes();
    }

    /**
     * Gets current storage usage as a percentage.
     *
     * @return Usage percentage (0-100+)
     */
    public double getUsagePercentage() {
        if (currentTier.getMaxBytes() == 0) {
            return 0;
        }
        return (estimatedUsageBytes * 100.0) / currentTier.getMaxBytes();
    }

    /**
     * Gets remaining storage bytes.
     *
     * @return Remaining bytes
     */
    public long getRemainingBytes() {
        return Math.max(0, currentTier.getMaxBytes() - estimatedUsageBytes);
    }

    /**
     * Gets remaining storage as formatted string.
     *
     * @return Formatted remaining storage (e.g., "512 MB")
     */
    public String getRemainingFormatted() {
        return formatBytes(getRemainingBytes());
    }

    /**
     * Gets current tier.
     *
     * @return Current StorageTier
     */
    public StorageTier getCurrentTier() {
        return currentTier;
    }

    /**
     * Sets the storage tier (simulates tier upgrade/downgrade).
     *
     * @param tier New tier to use
     */
    public void setTier(StorageTier tier) {
        if (tier != null) {
            this.currentTier = tier;
            logger.info("Storage tier changed to: {}", tier.getDisplayName());
        }
    }

    /**
     * Gets storage status information.
     *
     * @return Formatted status string
     */
    public String getStatus() {
        return String.format("%s: %s / %s (%.1f%% full)",
            currentTier.getDisplayName(),
            formatBytes(estimatedUsageBytes),
            formatBytes(currentTier.getMaxBytes()),
            getUsagePercentage());
    }

    /**
     * Formats bytes to human-readable string.
     *
     * @param bytes Number of bytes
     * @return Formatted string (e.g., "512 MB", "2 GB")
     */
    public static String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double displayValue = bytes;

        while (displayValue >= 1024 && unitIndex < units.length - 1) {
            displayValue /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", displayValue, units[unitIndex]);
    }

    /**
     * Gets estimated usage in bytes.
     *
     * @return Estimated usage
     */
    public long getEstimatedUsageBytes() {
        return estimatedUsageBytes;
    }

    /**
     * Gets maximum capacity in bytes for current tier.
     *
     * @return Maximum bytes
     */
    public long getMaxCapacityBytes() {
        return currentTier.getMaxBytes();
    }

    /**
     * Checks if auto-upgrade is enabled.
     *
     * @return true if auto-upgrade is enabled
     */
    public boolean isAutoUpgradeEnabled() {
        return autoUpgradeEnabled;
    }

    /**
     * Gets alert threshold percentage.
     *
     * @return Alert threshold (0-100)
     */
    public int getAlertThresholdPercent() {
        return alertThresholdPercent;
    }
}
