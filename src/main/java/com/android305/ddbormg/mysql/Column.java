package com.android305.ddbormg.mysql;

import java.math.BigDecimal;

public class Column {

    private String tableName;

    private String columnName;
    private String columnType;
    private int columnSize;
    private boolean nullable;
    private String defaultValue;
    private String remarks;

    public Column(String tableName, String columnName, String columnType, int columnSize, boolean nullable, String defaultValue, String remarks) {
        this.tableName = tableName;

        this.columnName = columnName;
        this.columnType = columnType;
        this.columnSize = columnSize;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.remarks = remarks;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public int getColumnSize() {
        return columnSize;
    }

    public void setColumnSize(int columnSize) {
        this.columnSize = columnSize;
    }

    public boolean nullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getJavaDefaultValue() {
        if (defaultValue == null && nullable())
            return "null;";
        switch (columnType) {
            case "INT":
            case "DOUBLE":
                return defaultValue + ';';
            case "VARCHAR":
                if (remarks != null && remarks.contains("BigDecimal")) {
                    BigDecimal bd = new BigDecimal(defaultValue);
                    if (bd.compareTo(BigDecimal.ZERO) == 0) {
                        return "BigDecimal.ZERO;";
                    } else if (bd.compareTo(BigDecimal.ONE) == 0) {
                        return "BigDecimal.ONE;";
                    } else if (bd.compareTo(BigDecimal.TEN) == 0) {
                        return "BigDecimal.TEN;";
                    }
                    return "new BigDecimal(\"" + defaultValue + "\");";
                }
            case "TEXT":
            case "MEDIUMTEXT":
                return '\"' + defaultValue + "\";";
            case "DATETIME":
            case "TIMESTAMP":
                if ("CURRENT_TIMESTAMP".equals(defaultValue)) {
                    return "; // CURRENT_TIMESTAMP";
                }
                return "Timestamp.valueOf(\"" + defaultValue + "\");";
            case "TIME":
                return "Time.valueOf(\"" + defaultValue + "\");";
            case "BIT":
            case "TINYINT":
                return defaultValue.equals("0") ? "false;" : "true;";
            case "DATE":
                return "Date.valueOf(\"" + defaultValue + "\");";
            default:
                throw new RuntimeException("Fix it felix. Unsupported column `" + columnName + "` in table `" + tableName + "`");
        }
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getRemarks() {
        return remarks;
    }

    public boolean avoidCache() {
        return getRemarks() != null && getRemarks().contains("no_cache");

    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getJavaClass() {
        switch (columnType) {
            case "INT":
                return nullable ? "Integer" : "int";
            case "DOUBLE":
                return nullable ? "Double" : "double";
            case "VARCHAR":
                if (remarks != null && remarks.contains("BigDecimal")) {
                    return "BigDecimal";
                }
            case "TEXT":
            case "MEDIUMTEXT":
                return "String";
            case "TIMESTAMP":
            case "DATETIME":
                return "Timestamp";
            case "TIME":
                return "Time";
            case "JSON":
                if (remarks != null) {
                    if (remarks.contains("JSONObject")) {
                        return "JSONObject";
                    } else if (remarks.contains("JSONArray")) {
                        return "JSONArray";
                    }
                }
                throw new RuntimeException("Fix it felix. Column `" + columnName + "` in table `" + tableName + "` is missing remarks to get type of json");
            case "BIT":
            case "TINYINT":
                return nullable ? "Boolean" : "boolean";
            case "DATE":
                return "Date";
            default:
                throw new RuntimeException("Fix it felix. Unsupported column `" + columnName + "` in table `" + tableName + "` of type `" + columnType + "`");
        }
    }

    public String getSQLiteClass() {
        switch (columnType) {
            case "BIT":
            case "TINYINT":
            case "INT":
                return "INTEGER";
            case "DOUBLE":
                return "DOUBLE";
            case "TIME":
            case "JSON":
            case "VARCHAR":
            case "TEXT":
            case "MEDIUMTEXT":
            case "DATE":
                return "TEXT";
            case "TIMESTAMP":
            case "DATETIME":
                return "DATETIME";
            default:
                throw new RuntimeException("Fix it felix. Unsupported column `" + columnName + "` in table `" + tableName + "`");
        }
    }

    public String getSQLiteGetter() {
        switch (columnType) {
            case "BIT":
            case "TINYINT":
            case "INT":
                return "getInt";
            case "DOUBLE":
                return "getDouble";
            case "TIME":
            case "JSON":
            case "VARCHAR":
            case "TIMESTAMP":
            case "DATETIME":
            case "DATE":
            case "TEXT":
            case "MEDIUMTEXT":
                return "getString";
            default:
                throw new RuntimeException("Fix it felix. Unsupported column `" + columnName + "` in table `" + tableName + "`");
        }
    }
}
