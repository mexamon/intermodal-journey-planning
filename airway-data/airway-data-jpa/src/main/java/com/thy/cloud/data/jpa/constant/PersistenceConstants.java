package com.thy.cloud.data.jpa.constant;

/**
 * Persistence Constants
 *
 * @author Engin Mahmut
 */
public final class PersistenceConstants {

    private PersistenceConstants() {
        throw new IllegalStateException("Constant class! Not any instance members");
    }

    public static final String BASE_ENTITY_PACKAGE = "com.thy.cloud.data.jpa.entity";

    public static final String BASE_REPOSITORY_PACKAGE = "com.thy.cloud.data.jpa.repository";

    /** BEGIN  JAVA BASIC TYPES **/
    public static final String BASIC_TYPE_INTEGER = "Integer";

    public static final String BASIC_TYPE_BIGDECIMAL = "BigDecimal";

    public static final String BASIC_TYPE_Long = "Long";

    public static final String BASIC_TYPE_DATE = "Date";

    public static final String BASIC_TYPE_INT = "int";

    public static final String BASIC_TYPE_STRING = "String";
    /** comma , */
    public static final String COMMA = ",";
    /** point . */
    public static final String DOT = ".";
    /** Semicolon ; */
    public static final String SEMICOLON = ";";

    /** double colon :: */
    public static final String DOUBLE_COLON = "::";

    /** minus sign - */
    public static final String MINUS = "-";

    /** underscore _ */
    public static final String UNDERLINE = "_";
    /** file separator / */
    public static final String SEPARATOR = "/";
    /** Left parenthesis( */
    public static final String LEFT_PARENTHESIS = "(";
    /** closing parenthesis) */
    public static final String RIGHT_PARENTHESIS = ")";
    /** Percent sign % */
    public static final String PERCENT_SIGN = "%";

}
