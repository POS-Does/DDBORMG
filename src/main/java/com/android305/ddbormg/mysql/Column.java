package com.android305.ddbormg.mysql;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;

public class Column {

    public final static String REMARK_NO_CACHE = "no_cache";

    private String tableName;

    private String columnName;
    private String columnType;
    private int columnSize;
    private boolean nullable;
    private String defaultValue;
    private String remarks;
    private HashSet<String> remarksSet;

    public Column(String tableName, ResultSet r) throws SQLException {
        this.tableName = tableName;

        columnName = r.getString("COLUMN_NAME");
        columnType = r.getString("TYPE_NAME");
        columnSize = r.getInt("COLUMN_SIZE");
        nullable = r.getInt("NULLABLE") == 1;
        defaultValue = r.getString("COLUMN_DEF");
        remarks = r.getString("REMARKS");
        if (remarks != null)
            remarksSet = new HashSet<>(Arrays.asList(remarks.split("\\|")));
    }

    public String getColumnName() {
        return columnName;
    }

    public String getColumnType() {
        switch (columnType) {
            case "MEDIUMTEXT":
                return "TEXT";
            default:
                return columnType;
        }
    }

    public int getColumnSize() {
        return columnSize;
    }

    public boolean nullable() {
        return nullable;
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
                if (hasRemark("BigDecimal")) {
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

    public String getRawRemarks() {
        return remarks;
    }

    public boolean hasRemark(String remark) {
        return remarks != null && remarksSet.contains(remark);
    }

    public String getJavaClass() {
        switch (columnType) {
            case "INT":
                return nullable ? "Integer" : "int";
            case "DOUBLE":
                return nullable ? "Double" : "double";
            case "VARCHAR":
                if (hasRemark("BigDecimal")) {
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
                if (hasRemark("JSONObject")) {
                    return "JSONObject";
                } else if (hasRemark("JSONArray")) {
                    return "JSONArray";
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
