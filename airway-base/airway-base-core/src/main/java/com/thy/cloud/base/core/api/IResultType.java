package com.thy.cloud.base.core.api;

/**
 * Result Types Standardization Interface
 *
 * @author Engin Mahmut
 * @version 1.1
 */

public interface IResultType {

    /**
     * Get the return code
     * @return return code
     */
    String getCode();

    /**
     * Get return information
     * @return return information
     */
    default String getMessage() {
        return "";
    }
}