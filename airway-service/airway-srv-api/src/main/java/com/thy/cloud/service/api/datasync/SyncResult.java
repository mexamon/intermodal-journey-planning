package com.thy.cloud.service.api.datasync;

import java.util.ArrayList;
import java.util.List;

/**
 * Result summary of a data sync operation.
 */
public record SyncResult(
        String sourceType,
        int edgesCreated,
        int edgesUpdated,
        int tripsCreated,
        int locationsCreated,
        int errors,
        List<String> warnings
) {
    public static Builder builder(String sourceType) {
        return new Builder(sourceType);
    }

    public static class Builder {
        private final String sourceType;
        private int edgesCreated, edgesUpdated, tripsCreated, locationsCreated, errors;
        private final List<String> warnings = new ArrayList<>();

        Builder(String sourceType) { this.sourceType = sourceType; }

        public Builder edgesCreated(int n)     { this.edgesCreated = n; return this; }
        public Builder edgesUpdated(int n)     { this.edgesUpdated = n; return this; }
        public Builder tripsCreated(int n)     { this.tripsCreated = n; return this; }
        public Builder locationsCreated(int n) { this.locationsCreated = n; return this; }
        public Builder errors(int n)           { this.errors = n; return this; }
        public Builder warn(String msg)        { this.warnings.add(msg); return this; }

        public SyncResult build() {
            return new SyncResult(sourceType, edgesCreated, edgesUpdated,
                    tripsCreated, locationsCreated, errors, warnings);
        }
    }
}
