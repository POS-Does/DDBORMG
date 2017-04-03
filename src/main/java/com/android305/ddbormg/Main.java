package com.android305.ddbormg;

import com.google.common.base.CaseFormat;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Main {

    public static void main(String... args) {
        try {
            new Main(args[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Incorrect type. One of --android or --php-api", e);
        }
    }

    public Main(String type) {
        try {
            type = type.replace("--", "");
            Connection conn = getConnection();
            DatabaseMetaData md = conn.getMetaData();
            ArrayList<String> cacheableTables = new ArrayList<>();
            switch (type) {
                case "android": {
                    ResultSet result = md.getTables(null, null, null, null);

                    while (result.next()) {
                        String tableName = result.getString("TABLE_NAME");
                        String remarks = result.getString("REMARKS");
                        boolean cached = remarks != null && remarks.contains("cache");
                        boolean skip = remarks != null && remarks.contains("no_api");
                        if (!skip) {
                            if (cached) {
                                cacheableTables.add(tableName);
                            }
                            System.out.println("Generating Class `" + tableName + "`...");
                            String className = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, tableName);
                            File dir = new File("app/src/main/java/com/android305/posdoes/rest/objects");
                            File javaFile = new File(dir, className + ".java");
                            File underscoreFile = new File(dir, className + "_.java");
                            System.out.println("Generating Class `" + tableName + "`...");
                            generateFile(javaFile, generateCacheableClass(md, tableName, className, cached));
                            System.out.println("Generating Class `" + tableName + "_`...");
                            generateFile(underscoreFile, generateUnderscoreClass(md, tableName, className, cached), true);
                        }
                    }
                    System.out.println("Generating Cache Controller...");
                    File dir = new File("app/src/main/java/com/android305/posdoes/rest/routes");
                    File cacheFile = new File(dir, "CacheController.java");
                    generateFile(cacheFile, generateCacheableClass(cacheableTables));

                    System.out.println("Generating Sqlite Class...");
                    dir = new File("app/src/main/java/com/android305/posdoes/service/sqlite");
                    File sqliteFile = new File(dir, "POSDbHelper.java");
                    generateFile(sqliteFile, generateSqliteClass(cacheableTables));

                    break;
                }
                case "php-api": {
                    ResultSet result = md.getTables(null, null, null, null);
                    while (result.next()) {
                        String tableName = result.getString("TABLE_NAME");
                        String remarks = result.getString("REMARKS");
                        if (remarks != null && remarks.contains("cache")) {
                            cacheableTables.add(tableName);
                        }
                    }
                    File dir = new File("src/v1/routes/");
                    File cacheFile = new File(dir, "cache.php");

                    generateFile(cacheFile, generatePhpCacheableController(md, cacheableTables));
                    break;
                }
                default:
                    throw new RuntimeException("Incorrect type. One of --android or --php-api");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void generateFile(File javaFile, String java) {
        generateFile(javaFile, java, false);
    }

    private void generateFile(File javaFile, String java, boolean ignore) {
        boolean write = false;
        try {
            List<String> lines = FileUtils.readLines(javaFile, "UTF-8");
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("// Generated")) {
                    write = true;
                    break;
                }

                if (i > 10)
                    break;
            }
        } catch (IOException ignored) {
            write = true;
        }
        if (write) {
            try {
                //String formattedSource = new Formatter().formatSource(java);
                FileUtils.writeStringToFile(javaFile, java, Charset.forName("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (!ignore)
                System.err.println("File " + javaFile.getPath() + " exists and is not a generated class.");
        }
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    private String generateCacheableClass(List<String> cachedTables) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated\n");
        sb.append("package com.android305.posdoes.rest.routes;\n");
        sb.append("import com.android305.posdoes.rest.RequestTypes;\n");
        sb.append("public class CacheController extends Controller {\n");
        sb.append("public CacheController() { super(\"cache\");}\n");

        for (String c : cachedTables) {
            String className = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, c);
            sb.append("public CacheController getAll" + className + "() {\n");
            sb.append("setPath(\"all/" + c.replace("_", "/") + "\");\n");
            sb.append("setRequestType(RequestTypes.GET);\n");
            sb.append("return this;\n");
            sb.append("}\n");
        }

        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    private String generateSqliteClass(List<String> cachedTables) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated\n");
        sb.append("package com.android305.posdoes.service.sqlite;\n");

        sb.append("import android.content.Context;\n");
        sb.append("import android.database.sqlite.SQLiteDatabase;\n");
        sb.append("import android.database.sqlite.SQLiteOpenHelper;\n");
        sb.append("import android.util.Log;\n");
        sb.append("import com.android305.posdoes.rest.exceptions.CompanyException;\n");
        sb.append("import com.android305.posdoes.rest.exceptions.DeviceException;\n");
        sb.append("import com.android305.posdoes.rest.utils.Encryption;\n");
        sb.append("import com.android305.posdoes.service.BackgroundService;\n");
        sb.append("import com.androidnetworking.error.ANError;\n");

        for (String c : cachedTables) {
            String className = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, c);
            sb.append("import com.android305.posdoes.rest.objects." + className + "_;\n");
        }

        sb.append("public class POSDbHelper extends SQLiteOpenHelper {\n");
        sb.append("private static final String TAG = POSDbHelper.class.getSimpleName();\n");
        sb.append("static final String DATABASE_NAME = \"posdb.db\";\n");
        sb.append("public POSDbHelper(Context context) { super(context, DATABASE_NAME, null, BackgroundService.DATABASE_VERSION); }\n");
        sb.append("@Override\n");
        sb.append("public void onCreate(SQLiteDatabase db) {\n");

        for (String c : cachedTables) {
            String className = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, c);
            sb.append("db.execSQL(" + className + "_.SQLiteHelper.SQL_CREATE_ENTRIES);\n");
        }
        sb.append("}\n");

        sb.append("@Override\n");
        sb.append("public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {\n");
        for (String c : cachedTables) {
            String className = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, c);
            sb.append("db.execSQL(" + className + "_.SQLiteHelper.SQL_DELETE_ENTRIES);\n");
        }
        sb.append("onCreate(db);\n}\n");

        sb.append("@Override\n");
        sb.append("public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) { onUpgrade(db, oldVersion, newVersion); }\n");

        sb.append("public static void resetLoadedStatus() {\n");
        for (String c : cachedTables) {
            String className = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, c);
            sb.append(className + "_.SQLiteHelper.loaded = null;\n");
        }
        sb.append("}\n");

        sb.append("public void loadCaches(BackgroundService service) throws DeviceException, Encryption.InvalidSecretKeyException, CompanyException, ANError {\n");

        for (String c : cachedTables) {
            String className = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, c);
            sb.append("if (" + className + "_.SQLiteHelper.loaded == null || service.getDevice().isCacheReset(" + className + "_.TABLE_NAME)) {\n");
            sb.append(className + "_.SQLiteHelper.loadAll(service);\n");
            sb.append("Log.v(TAG, \"Loaded \" + " + className + "_.TABLE_NAME + \" cache\");\n");
            sb.append("}\n");
        }
        sb.append("}\n");

        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    private String generatePhpCacheableController(DatabaseMetaData md, List<String> cachedTables) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?php\n");
        sb.append("// Generated\n");
        sb.append("// Cache\n");
        sb.append("$app->group('/api/v1/cache', function () use ($app) {\n");
        sb.append('\n');

        for (String c : cachedTables) {
            ArrayList<String> columns = new ArrayList<>();

            ResultSet r = md.getColumns(null, null, c, null);
            while (r.next()) {
                String columnName = r.getString("COLUMN_NAME");
                String columnType = r.getString("TYPE_NAME");
                int columnSize = r.getInt("COLUMN_SIZE");
                boolean nullable = r.getInt("NULLABLE") == 1;
                String defaultValue = r.getString("COLUMN_DEF");
                String remarks = r.getString("REMARKS");
                Column col = new Column(c, columnName, columnType, columnSize, nullable, defaultValue, remarks);

                if (!col.avoidCache())
                    columns.add(col.getColumnName());
            }

            String path = "/all/" + c.replace("_", "/");
            sb.append("    /**\n");
            sb.append("     * @EncryptedResponse\n");
            sb.append("     * @DeviceAuthenticationRequired\n");
            sb.append("     */\n");
            sb.append("    $app->get('" + path + "', function ($request, $response, $args) {\n");
            sb.append("        try {\n");
            sb.append("            $sth = $this->db->prepare(\"SELECT " + String.join(",", columns) + " FROM `" + c + "`\");\n");
            sb.append("            $sth->execute();\n");
            sb.append("            $payload = $sth->fetchAll();\n");
            sb.append("            $responseArray = array(\"payload\" => $payload);\n");
            sb.append("        } catch (\\Exception $e) {\n");
            sb.append("            $responseArray = array(\"error\" => \"UNKNOWN_ERROR\", \"error_msg\" => $e->getMessage());\n");
            sb.append("        }\n");
            sb.append("        return $this->response->withHeader('Encrypt', 'true')->withJson($responseArray);\n");
            sb.append("    });\n");
            sb.append('\n');
        }

        sb.append("});");
        return sb.toString();
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    private String generateUnderscoreClass(DatabaseMetaData md, String tableName, String className, boolean cached) throws SQLException {
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

        sb.append("package com.android305.posdoes.rest.objects;\n");

        sb.append("import com.android305.posdoes.service.BackgroundService;\n");

        sb.append("import com.android305.posdoes.rest.utils.json.JSONArray;\n");
        sb.append("import com.android305.posdoes.rest.utils.json.JSONObject;\n");
        sb.append("import com.android305.posdoes.service.BackgroundService;\n");

        sb.append("import java.io.Serializable;\n");
        sb.append("import java.sql.Timestamp;\n");
        sb.append("import java.sql.Time;\n");
        sb.append("import java.math.BigDecimal;\n");

        sb.append("public class " + className + "_ extends " + className + " implements Serializable {\n");

        // public ClassName_(BackgroundService service, {no defaults}) {
        {
            sb.append("public " + className + "_(BackgroundService service");
            for (int i = 3; i < columns.size(); i++) {
                Column c = columns.get(i);
                if (c.getDefaultValue() == null && !c.nullable()) {
                    sb.append(", " + c.getJavaClass() + " " + toLowerCamel(c.getColumnName()));
                }
            }
            sb.append(") {\n");
            sb.append("super(service, null, null, null");
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

        sb.append("public " + className + "_(BackgroundService service, JSONObject rawData) { super(service, rawData); }\n");
        sb.append('\n');

        sb.append("public " + className + "_(BackgroundService service, Integer " + toLowerCamel(columns.get(0).getColumnName()));
        for (int i = 1; i < columns.size(); i++) {
            Column c = columns.get(i);
            sb.append(", " + c.getJavaClass() + " " + toLowerCamel(c.getColumnName()));
        }
        sb.append(") {\n");
        sb.append("super(service");

        for (Column c : columns) {
            sb.append(", " + toLowerCamel(c.getColumnName()));
        }

        sb.append(");\n");
        sb.append("}\n");

        if (cached) {
            sb.append("public static class SQLiteHelper extends " + className + ".SQLiteHelper {\n");
            sb.append("}\n");
        }

        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    private String generateCacheableClass(DatabaseMetaData md, String tableName, String className, boolean cached) throws SQLException {
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

        sb.append("package com.android305.posdoes.rest.objects;\n");

        // Imports
        {
            sb.append("import com.android305.posdoes.service.BackgroundService;\n");

            sb.append("import android.content.ContentValues;\n");
            sb.append("import android.database.Cursor;\n");
            sb.append("import android.database.sqlite.SQLiteDatabase;\n");
            sb.append("import android.util.Log;\n");

            sb.append("import com.android305.posdoes.rest.exceptions.CompanyException;\n");
            sb.append("import com.android305.posdoes.rest.exceptions.DeviceException;\n");
            sb.append("import com.android305.posdoes.rest.routes.CacheController;\n");
            sb.append("import com.android305.posdoes.rest.utils.Encryption;\n");
            sb.append("import com.android305.posdoes.rest.utils.json.JSONArray;\n");
            sb.append("import com.android305.posdoes.rest.utils.json.JSONObject;\n");
            sb.append("import com.android305.posdoes.service.BackgroundService;\n");
            sb.append("import com.androidnetworking.error.ANError;\n");

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
        sb.append("public class " + className + " extends " + (cached ? "CachedObject" : "AndroidSQLObject") + " implements Serializable {\n");

        sb.append("static final String TAG = " + className + ".class.getSimpleName();\n");
        sb.append('\n');

        sb.append("/* Table Name */\n");
        sb.append("public static final String TABLE_NAME = \"" + tableName + "\";\n");
        sb.append('\n');

        // Column Tag Definitions
        {
            sb.append("/* Columns */\n");

            for (Column c : columns) {
                if (!"CREATED_TIME".equals(c.getColumnName()) && !"MODIFIED_TIME".equals(c.getColumnName())) {
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
            // ClassName(BackgroundService service, {no defaults}) {
            {
                sb.append(className + "(BackgroundService service");
                for (int i = 3; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    if (c.getDefaultValue() == null && !c.nullable()) {
                        sb.append(", " + c.getJavaClass() + " " + toLowerCamel(c.getColumnName()));
                    }
                }
                sb.append(") {\n");
                sb.append("this(service, null, null, null");
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

            // ClassName(BackgroundService service, JSONObject rawData) {
            sb.append(className + "(BackgroundService service, JSONObject rawData) { super(service, " + className + ".TABLE_NAME, rawData); load(rawData);}\n");
            sb.append('\n');

            // ClassName(BackgroundService service, {COLUMN_OBJECT_DEFINITIONS}) {
            {
                sb.append(className + "(BackgroundService service, Integer " + toLowerCamel(columns.get(0).getColumnName()));
                for (int i = 1; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    sb.append(", " + c.getJavaClass() + " " + toLowerCamel(c.getColumnName()));
                }
                sb.append(") {\n");
                sb.append("super(service, " + className + ".TABLE_NAME);\n");
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

        // String getIdColumnName() {
        {
            sb.append("@Override\n");
            sb.append("String getIdColumnName() { return " + columns.get(0).getColumnName() + ";}\n");
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
                    if (cached && c.avoidCache())
                        sb.append("if (isCached()) throw new RuntimeException(\"Cached object, this field is not cached\");\n");
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

        // static class SQLiteHelper {
        if (cached) {
            sb.append("static class SQLiteHelper {\n");
            // public static final String SQL_CREATE_ENTRIES = {SQLite COLUMN DATA};
            {
                sb.append("public static final String SQL_CREATE_ENTRIES = \"CREATE TABLE \" + " + className + ".TABLE_NAME + \" (\"\n");
                sb.append(" + " + className + "._ID + \" INTEGER PRIMARY KEY,\"\n");
                sb.append(" + " + className + "._CACHED + \" DATETIME,\"\n");
                sb.append(" + " + className + ".CREATED_TIME + \" DATETIME,\"\n");
                sb.append(" + " + className + ".MODIFIED_TIME + \" DATETIME,\"\n");

                for (int i = 3; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    if (!c.avoidCache()) {
                        sb.append(" + " + className + "." + c.getColumnName() + " + \" " + c.getSQLiteClass());

                        sb.append(",\"\n");
                    }
                }
                int lastComma = sb.toString().lastIndexOf(",");
                sb.replace(lastComma, lastComma + 1, ");");
                sb.append(";\n");
            }

            sb.append("public static final String SQL_DELETE_ENTRIES = \"DROP TABLE IF EXISTS \" + " + className + ".TABLE_NAME + \";\";\n");
            sb.append("public static final String SQL_TRUNCATE_ENTRIES = \"DELETE FROM \" + " + className + ".TABLE_NAME + \";\";\n\n");

            // static final String[] projection = {COLUMN DATA};
            {
                sb.append("static final String[] projection = {\n");
                sb.append(className + "._ID,\n");
                sb.append(className + "._CACHED,\n");
                sb.append(className + ".CREATED_TIME,\n");
                sb.append(className + ".MODIFIED_TIME,\n");

                for (int i = 3; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    if (!c.avoidCache()) {
                        sb.append(className + "." + c.getColumnName());
                        sb.append(",");
                        sb.append('\n');
                    }
                }
                int lastComma = sb.toString().lastIndexOf(",\n");
                sb.replace(lastComma, lastComma + 2, "");
                sb.append("};\n\n");
            }

            sb.append("public static Date loaded = null;\n\n");

            // public static ClassName_ getClassNameById(BackgroundService service, Integer id) {
            {
                sb.append("public static " + className + "_ get" + className + "ById(BackgroundService service, Integer id) {\n");
                sb.append("Log.v(TAG, \"Retrieved \" + TABLE_NAME + \" (\" + id + \")\");\n");
                sb.append("SQLiteDatabase db = service.getPOSDb().getReadableDatabase();\n");
                sb.append('\n');

                sb.append("String selection = " + className + "._ID + \" = ?\";\n");
                sb.append("String[] selectionArgs = {Integer.toString(id)};\n\n");

                sb.append("Cursor cursor = db.query(" + className + ".TABLE_NAME, projection, selection, selectionArgs, null, null, null);\n");
                sb.append("List<" + className + "_> list = convertCursor(service, cursor);\n");
                sb.append("if (list.size() > 0) return list.get(0);\n");
                sb.append("throw new RuntimeException(\"Id `\" + id + \"` for table `\" + TABLE_NAME + \"` not found.\");\n");
                sb.append("}\n");
            }

            // public static void loadAll(BackgroundService service) throws DeviceException, Encryption.InvalidSecretKeyException, CompanyException, ANError {
            {
                sb.append("public static void loadAll(BackgroundService service) throws DeviceException, Encryption.InvalidSecretKeyException, CompanyException, ANError {\n");
                sb.append("loaded = null;\n");
                sb.append("SQLiteDatabase db = service.getPOSDb().getWritableDatabase();\n");
                sb.append("db.execSQL(SQL_TRUNCATE_ENTRIES);\n");
                sb.append("JSONObject response = service.requestEncryptedJSON(new CacheController().getAll" + className + "(), true);\n");
                sb.append("Date now = new Date(System.currentTimeMillis());\n");
                sb.append("JSONArray payload = response.getJSONArray(\"payload\");\n");
                sb.append("db.beginTransaction();\n");
                sb.append("try {\n");
                sb.append("SimpleDateFormat dateFormat = new SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss\", Locale.getDefault());\n");
                sb.append("SimpleDateFormat timeFormat = new SimpleDateFormat(\"HH:mm:ss\", Locale.getDefault());\n");
                sb.append("for (int i = 0; i < payload.length(); i++) {\n");
                sb.append(className + " value = new " + className + "_(service, payload.getJSONObject(i));\n");
                sb.append("ContentValues cv = new ContentValues();\n");
                sb.append("cv.put(" + className + "._ID, value.getId());\n");
                sb.append("cv.put(" + className + "._CACHED, dateFormat.format(now));\n");
                sb.append("cv.put(" + className + ".CREATED_TIME, dateFormat.format(value.getCreatedTime()));\n");
                sb.append("cv.put(" + className + ".MODIFIED_TIME, dateFormat.format(value.getModifiedTime()));\n");

                for (int i = 3; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    if (!c.avoidCache()) {
                        String getter = "value.get" + toUpperCamel(c.getColumnName()) + "()";
                        sb.append("cv.put(" + className + "." + c.getColumnName() + ", " + getter);
                        switch (c.getJavaClass()) {
                            case "boolean":
                                sb.append(" ? 1 : 0");
                                break;
                            case "Boolean":
                                sb.append(" != null ? (" + getter + " ? 1 : 0) : null");
                                break;
                            case "Time":
                            case "Timestamp":
                            case "Date":
                            case "BigDecimal":
                            case "JSONObject":
                            case "JSONArray":
                                sb.append(" != null ? " + getter + ".toString() : null");
                                break;

                        }
                        sb.append(");\n");
                    }
                }

                sb.append("db.insert(" + className + ".TABLE_NAME, null, cv);\n");
                sb.append("}\n");
                sb.append("db.setTransactionSuccessful();\n");
                sb.append("} finally {\n");
                sb.append("db.endTransaction();\n");
                sb.append("loaded = now;\n");
                sb.append("}\n");
                sb.append("}\n\n");
            }

            // static List<ClassName_> convertCursor(BackgroundService service, Cursor cursor) {
            {
                sb.append("static List<" + className + "_> convertCursor(BackgroundService service, Cursor cursor) {\n");
                sb.append("ArrayList<" + className + "_> list = new ArrayList<>();\n");
                sb.append("while (cursor.moveToNext()) {\n");
                sb.append("int " + toLowerCamel(columns.get(0).getColumnName()) + " = cursor.getInt(cursor.getColumnIndexOrThrow(" + className + "._ID));\n");
                sb.append("Timestamp createdTime = Timestamp.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(" + className + ".CREATED_TIME)));\n");
                sb.append("Timestamp modifiedTime = Timestamp.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(" + className + ".MODIFIED_TIME)));\n");

                for (int i = 3; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    if (!c.avoidCache()) {
                        switch (c.getJavaClass()) {
                            case "int":
                            case "String":
                                sb.append(c.getJavaClass() + " " + toLowerCamel(c.getColumnName()) + " = cursor." + c.getSQLiteGetter() + "(cursor.getColumnIndexOrThrow(" + className + "." + c.getColumnName() + "));\n");
                                break;
                            case "Integer":
                                sb.append("Integer " + toLowerCamel(c.getColumnName()) + " = null;\n");
                                sb.append("if(!cursor.isNull(cursor.getColumnIndexOrThrow(" + className + "." + c.getColumnName() + "))) {");
                                sb.append(toLowerCamel(c.getColumnName()) + " = cursor.getInt(cursor.getColumnIndexOrThrow(" + className + "." + c.getColumnName() + "));");
                                sb.append("}\n");
                                break;
                            case "boolean":
                                sb.append("boolean " + toLowerCamel(c.getColumnName()) + " = cursor.getInt(cursor.getColumnIndexOrThrow(" + className + "." + c.getColumnName() + ")) == 1;\n");
                                break;
                            case "Boolean":
                                sb.append("Boolean " + toLowerCamel(c.getColumnName()) + " = null;\n");
                                sb.append("if(!cursor.isNull(cursor.getColumnIndexOrThrow(" + className + "." + c.getColumnName() + "))) {");
                                sb.append(toLowerCamel(c.getColumnName()) + " = cursor.getInt(cursor.getColumnIndexOrThrow(" + className + "." + c.getColumnName() + ")) == 1;");
                                sb.append("}\n");
                                break;
                            case "Time":
                            case "Timestamp":
                            case "Date":
                                if (c.nullable()) {
                                    sb.append("String " + toLowerCamel(c.getColumnName()) + "Val = cursor.getString(cursor.getColumnIndexOrThrow(" + className + "." + c.getColumnName() + "));\n");
                                    sb.append(c.getJavaClass() + " " + toLowerCamel(c.getColumnName()) + " = null;\n");
                                    sb.append("if(" + toLowerCamel(c.getColumnName()) + "Val != null) {\n");
                                    sb.append(toLowerCamel(c.getColumnName()) + " = " + c.getJavaClass() + ".valueOf(" + toLowerCamel(c.getColumnName()) + "Val);\n");
                                    sb.append("}\n");
                                } else {
                                    sb.append(c.getJavaClass() + " " + toLowerCamel(c.getColumnName()) + " = " + c.getJavaClass() + ".valueOf(cursor.getString(cursor.getColumnIndexOrThrow(" + className + "." + c
                                            .getColumnName() + ")));\n");
                                }
                                break;
                            case "BigDecimal":
                                if (c.nullable()) {
                                    sb.append("String " + toLowerCamel(c.getColumnName()) + "Val = cursor.getString(cursor.getColumnIndexOrThrow(" + className + "." + c.getColumnName() + "));\n");
                                    sb.append("BigDecimal " + toLowerCamel(c.getColumnName()) + " = null;\n");
                                    sb.append("if(" + toLowerCamel(c.getColumnName()) + "Val != null) {\n");
                                    sb.append(toLowerCamel(c.getColumnName()) + " = new BigDecimal(" + toLowerCamel(c.getColumnName()) + "Val);\n");
                                    sb.append("}\n");
                                } else {
                                    sb.append("BigDecimal " + toLowerCamel(c.getColumnName()) + " = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow(" + className + "." + c.getColumnName() + ")));\n");
                                }
                                break;
                            case "JSONObject":
                            case "JSONArray":
                                sb.append("String " + toLowerCamel(c.getColumnName()) + "Val = cursor.getString(cursor.getColumnIndexOrThrow(" + className + "." + c.getColumnName() + "));\n");
                                sb.append(c.getJavaClass() + " " + toLowerCamel(c.getColumnName()) + " = null;\n");
                                sb.append("if(" + toLowerCamel(c.getColumnName()) + "Val != null) {\n");
                                sb.append(toLowerCamel(c.getColumnName()) + " = new " + c.getJavaClass() + "(" + toLowerCamel(c.getColumnName()) + "Val);\n");
                                sb.append("}\n");
                                break;
                            default:
                                throw new RuntimeException("Fix it felix. Forgot about " + c.getJavaClass());
                        }
                    }
                }
                sb.append(className + "_ value = new " + className + "_(service");
                for (Column c : columns) {
                    if (!c.avoidCache()) {
                        sb.append(", " + toLowerCamel(c.getColumnName()));
                    } else {
                        String defaultValue = c.getJavaDefaultValue();
                        sb.append(", " + defaultValue.replace(";", ""));
                    }
                }
                sb.append(");\n");

                sb.append("value.setCached(Timestamp.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(" + className + "._CACHED))));\n");
                sb.append("list.add(value);\n");
                sb.append("}\n");
                sb.append(" cursor.close();\n");
                sb.append("return list;\n");
                sb.append("}\n");
                sb.append("}\n");
            }
        }
        sb.append('}');

        return sb.toString();
    }

    private String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    private String toLowerCamel(String st) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, st);
    }

    private String toUpperCamel(String st) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, st);
    }

    public Connection getConnection() throws SQLException {
        Properties connectionProps = new Properties();
        connectionProps.put("user", "root");
        connectionProps.put("password", "");
        connectionProps.setProperty("useInformationSchema", "true");
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/posdb", connectionProps);
    }
}