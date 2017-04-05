package com.android305.ddbormg.mysql;

public class ForeignKey {

    private String name;
    private String columnName;
    private String referencesTableName;
    private String referencesColumnName;

    public ForeignKey(String name, String columnName, String referencesTableName, String referencesColumnName) {
        this.name = name;
        this.columnName = columnName;
        this.referencesTableName = referencesTableName;
        this.referencesColumnName = referencesColumnName;
    }

    public String getName() {
        return name;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getReferencesTableName() {
        return referencesTableName;
    }

    public String getReferencesColumnName() {
        return referencesColumnName;
    }
}
