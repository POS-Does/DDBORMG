package com.android305.ddbormg.mysql;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Table {

    public final static String REMARK_NO_API = "no_api";
    public final static String REMARK_CACHE = "cache";

    private String name;
    private String remarks;
    private HashSet<String> remarksSet;

    private LinkedHashMap<String, Column> columns;
    private LinkedHashMap<String, ForeignKey> foreignKeys;
    private LinkedHashMap<String, Index> indexes;

    public Table(ResultSet result) throws SQLException {
        this.name = result.getString("TABLE_NAME");
        this.remarks = result.getString("REMARKS");
        this.remarksSet = new HashSet<>(Arrays.asList(remarks.split("\\|")));
    }

    public void loadColumns(DatabaseMetaData md) throws SQLException {
        columns = new LinkedHashMap<>();
        ResultSet r = md.getColumns(null, null, name, null);
        while (r.next()) {
            Column c = new Column(name, r);
            columns.put(c.getColumnName(), c);
        }
    }

    public void loadForeignKeys(DatabaseMetaData md) throws SQLException {
        foreignKeys = new LinkedHashMap<>();
        ResultSet rs = md.getImportedKeys(null, null, name);
        while (rs.next()) {
            ForeignKey fk = new ForeignKey(rs);
            foreignKeys.put(fk.getName(), fk);
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

    public String getName() {
        return name;
    }

    public String getRawRemarks() {
        return remarks;
    }

    public boolean hasRemark(String remark) {
        return remark != null && remarksSet.contains(remark);
    }
}
