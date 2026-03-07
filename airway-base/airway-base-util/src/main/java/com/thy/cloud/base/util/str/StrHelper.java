package com.thy.cloud.base.util.str;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class StrHelper {

    public static String getObjectValue(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    public static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getOrDef(String val, String def) {
        return (val == null || val.isEmpty()) ? def : val;
    }
}
