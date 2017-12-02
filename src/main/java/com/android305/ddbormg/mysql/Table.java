package com.android305.ddbormg.mysql;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Table {

    private String name;
    private String remarks;

    private LinkedHashMap<String, Column> columns;
    private LinkedHashMap<String, ForeignKey> foreignKeys;
    private LinkedHashMap<String, Index> indexes;

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
        columns = new LinkedHashMap<>();
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
        foreignKeys = new LinkedHashMap<>();
        ResultSet rs = md.getImportedKeys(null, null, name);
        while (rs.next()) {

            String fkName = rs.getString("FK_NAME");
            String fkColumnName = rs.getString("FKCOLUMN_NAME");
            String fkReferencesTableName = rs.getString("PKTABLE_NAME");
            String fkReferencesColumnName = rs.getString("PKCOLUMN_NAME");

            ForeignKey fk = new ForeignKey(fkName, fkColumnName, fkReferencesTableName, fkReferencesColumnName);

            foreignKeys.put(fkName, fk);
        }

        List<Map.Entry<String, ForeignKey>> entries = new ArrayList<>(foreignKeys.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        foreignKeys = new LinkedHashMap<>();
        for (Map.Entry<String, ForeignKey> entry : entries) {
            foreignKeys.put(entry.getKey(), entry.getValue());
        }
    }

    public void loadIndexes(DatabaseMetaData md) throws SQLException {
        indexes = new LinkedHashMap<>();
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

        List<Map.Entry<String, Index>> entries = new ArrayList<>(indexes.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        indexes = new LinkedHashMap<>();
        for (Map.Entry<String, Index> entry : entries) {
            indexes.put(entry.getKey(), entry.getValue());
        }
    }

    public HashMap<String, Column> getColumns() {
        return columns;
    }

    public List<Column> getColumnList() {
        return new ArrayList<>(columns.values());
    }

    public HashMap<String, ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public List<ForeignKey> getForeignKeyList() {
        return new ArrayList<>(foreignKeys.values());
    }

    public HashMap<String, Index> getIndexes() {
        return indexes;
    }
}
