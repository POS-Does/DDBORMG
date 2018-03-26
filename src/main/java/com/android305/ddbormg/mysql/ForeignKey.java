package com.android305.ddbormg.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ForeignKey {

    private String name;
    private String columnName;
    private String referencesTableName;
    private String referencesColumnName;

    ForeignKey(ResultSet rs) throws SQLException {
        name = rs.getString("FK_NAME");
        columnName = rs.getString("FKCOLUMN_NAME");
        referencesTableName = rs.getString("PKTABLE_NAME");
        referencesColumnName = rs.getString("PKCOLUMN_NAME");
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
