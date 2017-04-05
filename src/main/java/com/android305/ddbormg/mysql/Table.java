package com.android305.ddbormg.mysql;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class Table {

    private String name;
    private String remarks;

    private HashMap<String, Column> columns;
    private HashMap<String, ForeignKey> foreignKeys;
    private HashMap<String, Index> indexes;

    public Table(String name, String remarks) {
        this.name = name;
        this.remarks = remarks;
    }

    public String getName() {
        return name;
    }

    public String getRemarks() {
        return remarks;
    }

    public void loadColumns(DatabaseMetaData md) throws SQLException {
        columns = new HashMap<>();
        ResultSet r = md.getColumns(null, null, name, null);
        while (r.next()) {
            String columnName = r.getString("COLUMN_NAME");
            String columnType = r.getString("TYPE_NAME");
            int columnSize = r.getInt("COLUMN_SIZE");
            boolean nullable = r.getInt("NULLABLE") == 1;
            String defaultValue = r.getString("COLUMN_DEF");
            String remarks = r.getString("REMARKS");
            Column c = new Column(name, columnName, columnType, columnSize, nullable, defaultValue, remarks);
            columns.put(columnName, c);
        }
    }

    public void loadForeignKeys(DatabaseMetaData md) throws SQLException {
        foreignKeys = new HashMap<>();
        ResultSet rs = md.getImportedKeys(null, null, name);
        while (rs.next()) {

            String fkName = rs.getString("FK_NAME");
            String fkColumnName = rs.getString("FKCOLUMN_NAME");
            String fkReferencesTableName = rs.getString("PKTABLE_NAME");
            String fkReferencesColumnName = rs.getString("PKCOLUMN_NAME");

            if (fkReferencesColumnName.endsWith("_ID")) {
                fkReferencesColumnName = "ID";
            }

            ForeignKey fk = new ForeignKey(fkName, fkColumnName, fkReferencesTableName, fkReferencesColumnName);

            foreignKeys.put(fkName, fk);
        }
    }

    public void loadIndexes(DatabaseMetaData md) throws SQLException {
        indexes = new HashMap<>();
        ResultSet rs = md.getIndexInfo(null, null, name, false, false);
        while (rs.next()) {
            boolean unique = !rs.getBoolean("NON_UNIQUE");
            String indexName = rs.getString("INDEX_NAME");
            String columnName = rs.getString("COLUMN_NAME");
            if (!indexName.equals("PRIMARY")) {
                Index index = indexes.get(indexName);
                if (index == null) {
                    index = new Index(indexName, unique);
                }
                index.addColumn(columnName);
                indexes.put(indexName, index);
            }
        }
    }

    public HashMap<String, Column> getColumns() {
        return columns;
    }

    public HashMap<String, ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public HashMap<String, Index> getIndexes() {
        return indexes;
    }
}
