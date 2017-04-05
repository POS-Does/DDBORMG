package com.android305.ddbormg.utils;

import com.google.common.base.CaseFormat;

public class JavaUtils {

    public static String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    public static String toLowerCamel(String st) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, st);
    }

    public static String toUpperCamel(String st) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, st);
    }
}
