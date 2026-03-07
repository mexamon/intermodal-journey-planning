package com.thy.cloud.data.jpa.specs.alternate;

import java.io.Serializable;

import lombok.Data;

/**
 * Operator class, which stores key-value pairs and operation symbols, whether the condition type of connection is and or or
 * Pass id>=7 when creating, where id is the key, >= is the operator, and 7 is the value
 * Special custom operators (: means like %v%, b: means v%, :b means %v)
 *
 */
@Data
public class SpecificationOperator implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 837938919256998640L;

    /**
     * Attributes, such as the name and id of the query
     */
    private String key;

    /**
     * Specific query conditions
     */
    private transient Object value;

    /**
     * Operator, a set of operators defined by oneself, used to facilitate query
     */
    private Operator operator;

    /**
     * Connection method: and, or
     */
    private String join;
}