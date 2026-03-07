package com.thy.cloud.base.core.config;

public class Airway {

    private static final String VERSION = "${version}";

    private Airway() {
    }

    /**
     * Get Version
     *
     * @return Version
     */
    public static String getVersion() {
        return VERSION;
    }
}
