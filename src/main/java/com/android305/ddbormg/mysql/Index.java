package com.android305.ddbormg.mysql;

import java.util.ArrayList;
import java.util.Arrays;

public class Index {

    private String name;
    private boolean unique;
    private ArrayList<String> columns;

    public Index(String name, boolean unique) {
        this.name = name;
        this.unique = unique;
        columns = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public boolean isUnique() {
        return unique;
    }

    public ArrayList<String> getColumns() {
        return columns;
    }

    public void addColumn(String column) {
        columns.add(column);
    }

    @Override
    public String toString() {
        return "Index{" + "name='" + name + '\'' + ", unique=" + unique + ", columns=" + Arrays.toString(columns.toArray()) + '}';
    }
}
