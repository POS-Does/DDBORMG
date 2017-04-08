package com.android305.ddbormg.generators;

import com.android305.ddbormg.mysql.Column;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static com.android305.ddbormg.utils.JavaUtils.capitalize;
import static com.android305.ddbormg.utils.JavaUtils.toLowerCamel;
import static com.android305.ddbormg.utils.JavaUtils.toUpperCamel;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class JavaGenerator {

    public static String generateUnderscoreClass(String appPackageName, DatabaseMetaData md, String tableName, String className) throws SQLException {
        StringBuilder sb = new StringBuilder();
        ArrayList<Column> columns = new ArrayList<>();

        ResultSet r = md.getColumns(null, null, tableName, null);
        while (r.next()) {
            String columnName = r.getString("COLUMN_NAME");
            String columnType = r.getString("TYPE_NAME");
            int columnSize = r.getInt("COLUMN_SIZE");
            boolean nullable = r.getInt("NULLABLE") == 1;
            String defaultValue = r.getString("COLUMN_DEF");
            String remarks = r.getString("REMARKS");

            columns.add(new Column(tableName, columnName, columnType, columnSize, nullable, defaultValue, remarks));
        }
        sb.append("// Generated\n");

        sb.append("package " + appPackageName + ".rest.objects;\n");

        sb.append("import com.android305.posdoes.utils.json.JSONArray;\n");
        sb.append("import com.android305.posdoes.utils.json.JSONObject;\n");

        sb.append("import java.io.Serializable;\n");
        sb.append("import java.sql.Timestamp;\n");
        sb.append("import java.sql.Time;\n");
        sb.append("import java.math.BigDecimal;\n");

        sb.append("public class " + className + "_ extends " + className + " implements Serializable {\n");

        // public ClassName_({no defaults}) {
        {
            sb.append("public " + className + "_(");
            for (int i = 3; i < columns.size(); i++) {
                Column c = columns.get(i);
                if (c.getDefaultValue() == null && !c.nullable()) {
                    sb.append(c.getJavaClass() + " " + toLowerCamel(c.getColumnName()) + ",");
                }
            }
            int lastComma = sb.toString().lastIndexOf(",");
            if (lastComma > -1) {
                sb.replace(lastComma, lastComma + 1, "");
            }
            sb.append(") {\n");
            sb.append("super(null, null, null");
            for (int i = 3; i < columns.size(); i++) {
                Column c = columns.get(i);
                if (c.getDefaultValue() == null && !c.nullable()) {
                    sb.append(", " + toLowerCamel(c.getColumnName()));
                } else {
                    String defaultValue = c.getJavaDefaultValue();
                    sb.append(", " + defaultValue.substring(0, defaultValue.length() - 1));
                }
            }
            sb.append(");\n");
            sb.append("}\n\n");
        }

        sb.append("public " + className + "_(JSONObject rawData) { super(rawData); }\n");
        sb.append('\n');

        sb.append("public " + className + "_(Integer " + toLowerCamel(columns.get(0).getColumnName()));
        for (int i = 1; i < columns.size(); i++) {
            Column c = columns.get(i);
            sb.append(", " + c.getJavaClass() + " " + toLowerCamel(c.getColumnName()));
        }
        sb.append(") {\n");
        sb.append("super(");

        for (Column c : columns) {
            sb.append(toLowerCamel(c.getColumnName()) + ",");
        }
        int lastComma = sb.toString().lastIndexOf(",");
        sb.replace(lastComma, lastComma + 1, "");

        sb.append(");\n");
        sb.append("}\n");

        sb.append("}");
        return sb.toString();
    }

    public static String generateClass(String appPackageName, DatabaseMetaData md, String tableName, String className) throws SQLException {
        StringBuilder sb = new StringBuilder();
        ArrayList<Column> columns = new ArrayList<>();

        ResultSet r = md.getColumns(null, null, tableName, null);
        while (r.next()) {
            String columnName = r.getString("COLUMN_NAME");
            String columnType = r.getString("TYPE_NAME");
            int columnSize = r.getInt("COLUMN_SIZE");
            boolean nullable = r.getInt("NULLABLE") == 1;
            String defaultValue = r.getString("COLUMN_DEF");
            String remarks = r.getString("REMARKS");
            Column c = new Column(tableName, columnName, columnType, columnSize, nullable, defaultValue, remarks);
            columns.add(c);
        }
        sb.append("// Generated\n");

        sb.append("package " + appPackageName + ".rest.objects;\n");

        // Imports
        {
            sb.append("import com.android305.posdoes.utils.json.JSONArray;\n");
            sb.append("import com.android305.posdoes.utils.json.JSONObject;\n");

            sb.append("import java.io.Serializable;\n");
            sb.append("import java.sql.Timestamp;\n");
            sb.append("import java.sql.Time;\n");
            sb.append("import java.text.SimpleDateFormat;\n");
            sb.append("import java.util.ArrayList;\n");
            sb.append("import java.util.Date;\n");
            sb.append("import java.util.List;\n");
            sb.append("import java.util.Locale;\n");
            sb.append("import java.math.BigDecimal;\n");
        }

        // public class ClassName extends {CachedObject|AndroidSQLObject} {
        sb.append("public class " + className + " extends SQLObject implements Serializable {\n");

        sb.append("static final String TAG = " + className + ".class.getSimpleName();\n");
        sb.append('\n');

        sb.append("/* Table Name */\n");
        sb.append("public static final String TABLE_NAME = \"" + tableName + "\";\n");
        sb.append('\n');

        // Column Tag Definitions
        {
            sb.append("/* Columns */\n");

            for (int i = 3; i < columns.size(); i++) {
                Column c = columns.get(i);
                sb.append("public static final String " + c.getColumnName() + " = \"" + c.getColumnName() + "\"; // " + c.getColumnType());
                switch (c.getColumnType()) {
                    case "VARCHAR":
                        sb.append("(" + c.getColumnSize() + ")");
                        break;
                    case "INT":
                        sb.append("(" + (c.getColumnSize() + 1) + ")");
                        break;
                }
                if (c.nullable()) {
                    sb.append(" NULLABLE");
                }
                sb.append('\n');
            }
            sb.append('\n');
        }

        // Column Object Definitions
        {
            for (int i = 3; i < columns.size(); i++) {
                Column c = columns.get(i);

                sb.append("private " + c.getJavaClass() + " " + toLowerCamel(c.getColumnName()));

                if (c.nullable() || c.getDefaultValue() != null) {
                    sb.append(" = " + c.getJavaDefaultValue() + '\n');
                } else {
                    sb.append(";\n");
                }
            }
            sb.append('\n');
        }

        // Constructors
        {
            // ClassName({no defaults}) {
            {
                sb.append(className + "(");
                for (int i = 3; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    if (c.getDefaultValue() == null && !c.nullable()) {
                        sb.append(", " + c.getJavaClass() + " " + toLowerCamel(c.getColumnName()));
                    }
                }
                sb.append(") {\n");
                sb.append("this(null, null, null");
                for (int i = 3; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    if (c.getDefaultValue() == null && !c.nullable()) {
                        sb.append(", " + toLowerCamel(c.getColumnName()));
                    } else {
                        String defaultValue = c.getJavaDefaultValue();
                        sb.append(", " + defaultValue.substring(0, defaultValue.length() - 1));
                    }
                }
                sb.append(");\n");
                sb.append("}\n\n");
            }

            // ClassName(JSONObject rawData) {
            sb.append(className + "(JSONObject rawData) { super(" + className + ".TABLE_NAME, rawData); load(rawData);}\n");
            sb.append('\n');

            // ClassName({COLUMN_OBJECT_DEFINITIONS}) {
            {
                sb.append(className + "(Integer " + toLowerCamel(columns.get(0).getColumnName()));
                for (int i = 1; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    sb.append(", " + c.getJavaClass() + " " + toLowerCamel(c.getColumnName()));
                }
                sb.append(") {\n");
                sb.append("super(" + className + ".TABLE_NAME);\n");
                sb.append('\n');

                for (Column c : columns) {
                    sb.append("mRawData.put(" + c.getColumnName() + ", " + toLowerCamel(c.getColumnName()) + ");\n");
                }
                sb.append('\n');

                sb.append("load(mRawData);}\n");
                sb.append('\n');
            }
        }

        // protected void loadData() {
        {
            sb.append("@Override\n");
            sb.append("protected void loadData() {\n");

            for (int i = 3; i < columns.size(); i++) {
                Column c = columns.get(i);
                String getter = "get";
                if (c.avoidCache())
                    getter = "opt";
                sb.append("set" + toUpperCamel(c.getColumnName()) + "(mRawData." + getter + capitalize(c.getJavaClass()) + "(" + c.getColumnName() + "));\n");
            }
            sb.append("}\n");
            sb.append('\n');
        }

        // Getters and setters
        {
            for (int i = 3; i < columns.size(); i++) {
                Column c = columns.get(i);

                String upper = toUpperCamel(c.getColumnName());
                String camel = toLowerCamel(c.getColumnName());

                // Getter
                {
                    sb.append("public " + c.getJavaClass() + " get" + upper + "() { \n");
                    sb.append("return " + camel + ";}\n");
                    sb.append('\n');
                }

                // Setter
                {

                    sb.append("public void set" + upper + "(" + c.getJavaClass() + " " + camel + ") { \n");
                    sb.append("this.mRawData.put(" + c.getColumnName() + ", " + camel);

                    switch (c.getJavaClass()) {
                        case "Boolean":
                            sb.append(" != null ? (" + camel + " ? 1 : 0) : null");
                            break;
                        case "boolean":
                            sb.append("? 1 : 0");
                            break;
                    }
                    sb.append(");\n");

                    sb.append("this." + camel + " = " + camel + ";\n}\n");
                    sb.append('\n');
                }
            }
        }

        // public boolean equals() {
        {
            sb.append("@SuppressWarnings({\"SimplifiableIfStatement\", \"RedundantIfStatement\"})\n");
            sb.append("@Override\n");
            sb.append("public boolean equals(Object o) {\n");
            sb.append("if (this == o) return true;\n");
            sb.append("if (!(o instanceof " + className + ")) return false;\n");

            // ClassName className = (ClassName) o;
            sb.append(className + " " + toLowerCamel(className) + " = (" + className + ") o;\n");

            sb.append("if(!super.equals(" + toLowerCamel(className) + ")) return false;\n");

            for (int i = 3; i < columns.size(); i++) {
                Column c = columns.get(i);
                String upper = toUpperCamel(c.getColumnName());
                String get = " get" + upper + "()";

                switch (c.getJavaClass()) {
                    case "int":
                    case "boolean":
                    case "double":
                        // if (getter() != className.getter())
                        sb.append("if(" + get + " != " + toLowerCamel(className) + "." + get + ") return false;\n");
                        break;
                    default:
                        // if (getter() != null ? !getter().equals(className.getter()) : className.getter() != null)
                        sb.append("if(" + get + " != null ? !" + get + ".equals(" + toLowerCamel(className) + "." + get + ") : " + toLowerCamel(className) + "." + get + " != null) return false;\n");
                        break;
                }
            }
            sb.append("return true;\n");
            sb.append("}\n\n");
        }
        sb.append('}');

        return sb.toString();
    }
}
