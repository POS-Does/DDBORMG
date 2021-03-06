package com.android305.ddbormg.generators;

import com.android305.ddbormg.mysql.Column;
import com.android305.ddbormg.mysql.ForeignKey;
import com.android305.ddbormg.mysql.Table;
import com.google.common.base.CaseFormat;

import java.util.List;

import static com.android305.ddbormg.mysql.Column.REMARK_NO_CACHE;
import static com.android305.ddbormg.utils.JavaUtils.capitalize;
import static com.android305.ddbormg.utils.JavaUtils.toLowerCamel;
import static com.android305.ddbormg.utils.JavaUtils.toUpperCamel;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class AndroidGenerator {

    public static String generateControllerClass(List<String> cachedTables) {
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

    public static String generateSqliteClass(List<String> cachedTables) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated\n");
        sb.append("package com.android305.posdoes.service.sqlite;\n");

        sb.append("import android.content.Context;\n");
        sb.append("import android.database.sqlite.SQLiteDatabase;\n");
        sb.append("import android.database.sqlite.SQLiteOpenHelper;\n");
        sb.append("import android.util.Log;\n");
        sb.append("import com.android305.posdoes.rest.exceptions.CompanyException;\n");
        sb.append("import com.android305.posdoes.rest.exceptions.DeviceException;\n");
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

        sb.append("public void loadCaches(BackgroundService service) throws DeviceException, CompanyException, ANError {\n");

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

    public static String generateUnderscoreClass(Table table, String className, boolean cached) {
        StringBuilder sb = new StringBuilder();
        List<Column> columns = table.getColumnList();

        sb.append("// Generated\n");

        sb.append("package com.android305.posdoes.rest.objects;\n");

        sb.append("import com.android305.posdoes.service.BackgroundService;\n");

        sb.append("import com.android305.posdoes.utils.json.JSONArray;\n");
        sb.append("import com.android305.posdoes.utils.json.JSONObject;\n");
        sb.append("import com.android305.posdoes.service.BackgroundService;\n");

        sb.append("import java.io.Serializable;\n");
        sb.append("import java.sql.Timestamp;\n");
        sb.append("import java.sql.Time;\n");
        sb.append("import java.math.BigDecimal;\n");

        sb.append("@SuppressWarnings(\"WeakerAccess\")\n");
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

    public static String generateClass(Table table, String className, boolean cached) {
        StringBuilder sb = new StringBuilder();
        List<Column> columns = table.getColumnList();

        sb.append("// Generated\n");

        sb.append("package com.android305.posdoes.rest.objects;\n");

        // Imports
        {
            sb.append("import com.android305.posdoes.service.BackgroundService;\n");

            sb.append("import android.content.ContentValues;\n");
            sb.append("import android.database.Cursor;\n");
            sb.append("import android.database.sqlite.SQLiteDatabase;\n");
            sb.append("import android.util.Log;\n");
            sb.append("import android.util.SparseArray;\n");

            sb.append("import com.android305.posdoes.rest.exceptions.CompanyException;\n");
            sb.append("import com.android305.posdoes.rest.exceptions.DeviceException;\n");
            sb.append("import com.android305.posdoes.rest.routes.CacheController;\n");
            sb.append("import com.android305.posdoes.utils.json.JSONArray;\n");
            sb.append("import com.android305.posdoes.utils.json.JSONObject;\n");
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

        sb.append("@SuppressWarnings(\"WeakerAccess\")\n");
        // public class ClassName extends {CachedObject|AndroidSQLObject} {
        sb.append("public class " + className + " extends " + (cached ? "CachedObject" : "AndroidSQLObject") + " implements Serializable {\n");

        sb.append("static final String TAG = " + className + ".class.getSimpleName();\n");
        sb.append('\n');

        sb.append("/* Table Name */\n");
        sb.append("public static final String TABLE_NAME = \"" + table.getName() + "\";\n");
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
            sb.append(className + "(BackgroundService service, JSONObject rawData) { super(service, " + className + ".TABLE_NAME); load(rawData);}\n");
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
                if (c.hasRemark(REMARK_NO_CACHE))
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
                    if (cached && c.hasRemark(REMARK_NO_CACHE))
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

        // static class SQLiteHelper {
        if (cached) {
            sb.append("static class SQLiteHelper {\n");
            // public static final String SQL_CREATE_ENTRIES = {SQLite COLUMN DATA};
            {
                sb.append("        // @formatter:off\n");
                sb.append("        public static final String SQL_CREATE_ENTRIES = \"CREATE TABLE " + table.getName() + " (\"\n");
                sb.append("                + \"_id INTEGER PRIMARY KEY,\"\n");
                sb.append("                + \"_CACHED DATETIME,\"\n");
                sb.append("                + \"CREATED_TIME DATETIME,\"\n");
                sb.append("                + \"MODIFIED_TIME DATETIME,\"\n");

                for (int i = 3; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    if (!c.hasRemark(REMARK_NO_CACHE)) {
                        // + "COLUMN_NAME TEXT,"
                        sb.append("                + \"" + c.getColumnName() + " " + c.getSQLiteClass() + ",\"");
                        sb.append('\n');
                    }
                }
                List<ForeignKey> foreignKeyList = table.getForeignKeyList();

                for (ForeignKey fk : foreignKeyList) {
                    // + "FOREIGN KEY(COLUMN_NAME) REFERENCES reference_table_name(REFERENCE_COLUMN_NAME),
                    sb.append("                + \"FOREIGN KEY(" + fk.getColumnName() + ") REFERENCES " + fk.getReferencesTableName() + "(" + fk.getReferencesColumnName() + "),\"");
                    sb.append('\n');
                }

                int lastComma = sb.toString().lastIndexOf(",\"\n");
                sb.replace(lastComma, lastComma + 3, ");\"");
                sb.append(";\n");
                sb.append("        // @formatter:on\n\n");
            }

            sb.append("public static final String SQL_DELETE_ENTRIES = \"DROP TABLE IF EXISTS \" + TABLE_NAME + \";\";\n");
            sb.append("public static final String SQL_TRUNCATE_ENTRIES = \"DELETE FROM \" + TABLE_NAME + \";\";\n\n");

            // static final String[] projection = {COLUMN DATA};
            {
                sb.append("static final String[] projection = {\n");
                sb.append("_ID,\n");
                sb.append("_CACHED,\n");
                sb.append("CREATED_TIME,\n");
                sb.append("MODIFIED_TIME,\n");

                for (int i = 3; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    if (!c.hasRemark(REMARK_NO_CACHE)) {
                        sb.append(c.getColumnName());
                        sb.append(",");
                        sb.append('\n');
                    }
                }
                int lastComma = sb.toString().lastIndexOf(",\n");
                sb.replace(lastComma, lastComma + 2, "");
                sb.append("};\n\n");
            }

            sb.append("static SparseArray<" + className + "_> cached = new SparseArray<>();\n");

            sb.append("public static Date loaded = null;\n\n");

            // public static ClassName_ getClassNameById(BackgroundService service, Integer id) {
            {
                sb.append("public static " + className + "_ get" + className + "ById(BackgroundService service, Integer id) {\n");
                sb.append("if(cached.get(id) != null) return cached.get(id);\n");
                sb.append("Log.v(TAG, \"Retrieved \" + TABLE_NAME + \" (\" + id + \")\");\n");
                sb.append("SQLiteDatabase db = service.getPOSDb().getReadableDatabase();\n");
                sb.append('\n');

                sb.append("String selection = _ID + \" = ?\";\n");
                sb.append("String[] selectionArgs = {Integer.toString(id)};\n\n");

                sb.append("Cursor cursor = db.query(TABLE_NAME, projection, selection, selectionArgs, null, null, null);\n");
                sb.append("List<" + className + "_> list = convertCursor(service, cursor);\n");
                sb.append("if (list.size() > 0) return list.get(0);\n");
                sb.append("throw new RuntimeException(\"Id `\" + id + \"` for table `\" + TABLE_NAME + \"` not found.\");\n");
                sb.append("}\n");
            }

            // public static void loadAll(BackgroundService service) throws DeviceException, CompanyException, ANError {
            {
                sb.append("public static void loadAll(BackgroundService service) throws DeviceException, CompanyException, ANError {\n");
                sb.append("cached.clear();\n");
                sb.append("loaded = null;\n");
                sb.append("SQLiteDatabase db = service.getPOSDb().getWritableDatabase();\n");
                sb.append("db.execSQL(SQL_TRUNCATE_ENTRIES);\n");
                sb.append("JSONObject response = service.request(new CacheController().getAll" + className + "(), true);\n");
                sb.append("Date now = new Date(System.currentTimeMillis());\n");
                sb.append("JSONArray payload = response.getJSONArray(\"payload\");\n");
                sb.append("db.beginTransaction();\n");
                sb.append("try {\n");
                sb.append("SimpleDateFormat dateFormat = new SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss\", Locale.getDefault());\n");
                sb.append("SimpleDateFormat timeFormat = new SimpleDateFormat(\"HH:mm:ss\", Locale.getDefault());\n");
                sb.append("for (int i = 0; i < payload.length(); i++) {\n");
                sb.append("JSONObject value = payload.getJSONObject(i);\n");
                sb.append("ContentValues cv = new ContentValues();\n");
                sb.append("cv.put(_ID, value.getInt(ID));\n");
                sb.append("cv.put(_CACHED, dateFormat.format(now));\n");
                sb.append("cv.put(CREATED_TIME, dateFormat.format(value.getTimestamp(CREATED_TIME)));\n");
                sb.append("if(value.getTimestamp(MODIFIED_TIME) != null) {\n");
                sb.append("cv.put(MODIFIED_TIME, dateFormat.format(value.getTimestamp(MODIFIED_TIME)));\n");
                sb.append("} else {\n");
                sb.append("cv.put(MODIFIED_TIME, (String) null);\n");
                sb.append("}\n");

                for (int i = 3; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    if (!c.hasRemark(REMARK_NO_CACHE)) {
                        String getter = "value.get" + capitalize(c.getJavaClass()) + "(" + c.getColumnName() + ")";
                        sb.append("cv.put(" + c.getColumnName() + ", " + getter);
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

                sb.append("db.insert(TABLE_NAME, null, cv);\n");
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
                sb.append("int " + toLowerCamel(columns.get(0).getColumnName()) + " = cursor.getInt(cursor.getColumnIndexOrThrow(_ID));\n");
                sb.append("Timestamp createdTime = Timestamp.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(CREATED_TIME)));\n");
                sb.append("Timestamp modifiedTime = null;\n");
                sb.append("if(!cursor.isNull(cursor.getColumnIndexOrThrow(MODIFIED_TIME))) {");
                sb.append("modifiedTime = Timestamp.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(MODIFIED_TIME)));\n");
                sb.append("}\n");
                for (int i = 3; i < columns.size(); i++) {
                    Column c = columns.get(i);
                    if (!c.hasRemark(REMARK_NO_CACHE)) {
                        switch (c.getJavaClass()) {
                            case "int":
                            case "String":
                                sb.append(c.getJavaClass() + " " + toLowerCamel(c.getColumnName()) + " = cursor." + c.getSQLiteGetter() + "(cursor.getColumnIndexOrThrow(" + c.getColumnName() + "));\n");
                                break;
                            case "Integer":
                            case "Double":
                                sb.append(c.getJavaClass() + " " + toLowerCamel(c.getColumnName()) + " = null;\n");
                                sb.append("if(!cursor.isNull(cursor.getColumnIndexOrThrow(" + c.getColumnName() + "))) {");
                                sb.append(toLowerCamel(c.getColumnName()) + " = cursor." + c.getSQLiteGetter() + "(cursor.getColumnIndexOrThrow(" + c.getColumnName() + "));");
                                sb.append("}\n");
                                break;
                            case "boolean":
                                sb.append("boolean " + toLowerCamel(c.getColumnName()) + " = cursor.getInt(cursor.getColumnIndexOrThrow(" + c.getColumnName() + ")) == 1;\n");
                                break;
                            case "Boolean":
                                sb.append("Boolean " + toLowerCamel(c.getColumnName()) + " = null;\n");
                                sb.append("if(!cursor.isNull(cursor.getColumnIndexOrThrow(" + c.getColumnName() + "))) {");
                                sb.append(toLowerCamel(c.getColumnName()) + " = cursor.getInt(cursor.getColumnIndexOrThrow(" + c.getColumnName() + ")) == 1;");
                                sb.append("}\n");
                                break;
                            case "Time":
                            case "Timestamp":
                            case "Date":
                                if (c.nullable()) {
                                    sb.append("String " + toLowerCamel(c.getColumnName()) + "Val = cursor.getString(cursor.getColumnIndexOrThrow(" + c.getColumnName() + "));\n");
                                    sb.append(c.getJavaClass() + " " + toLowerCamel(c.getColumnName()) + " = null;\n");
                                    sb.append("if(" + toLowerCamel(c.getColumnName()) + "Val != null) {\n");
                                    sb.append(toLowerCamel(c.getColumnName()) + " = " + c.getJavaClass() + ".valueOf(" + toLowerCamel(c.getColumnName()) + "Val);\n");
                                    sb.append("}\n");
                                } else {
                                    sb.append(c.getJavaClass() + " " + toLowerCamel(c.getColumnName()) + " = " + c.getJavaClass() + ".valueOf(cursor.getString(cursor.getColumnIndexOrThrow(" + c.getColumnName() + ")));\n");
                                }
                                break;
                            case "BigDecimal":
                                if (c.nullable()) {
                                    sb.append("String " + toLowerCamel(c.getColumnName()) + "Val = cursor.getString(cursor.getColumnIndexOrThrow(" + c.getColumnName() + "));\n");
                                    sb.append("BigDecimal " + toLowerCamel(c.getColumnName()) + " = null;\n");
                                    sb.append("if(" + toLowerCamel(c.getColumnName()) + "Val != null) {\n");
                                    sb.append(toLowerCamel(c.getColumnName()) + " = new BigDecimal(" + toLowerCamel(c.getColumnName()) + "Val);\n");
                                    sb.append("}\n");
                                } else {
                                    sb.append("BigDecimal " + toLowerCamel(c.getColumnName()) + " = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow(" + c.getColumnName() + ")));\n");
                                }
                                break;
                            case "JSONObject":
                            case "JSONArray":
                                sb.append("String " + toLowerCamel(c.getColumnName()) + "Val = cursor.getString(cursor.getColumnIndexOrThrow(" + c.getColumnName() + "));\n");
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
                    if (!c.hasRemark(REMARK_NO_CACHE)) {
                        sb.append(", " + toLowerCamel(c.getColumnName()));
                    } else {
                        String defaultValue = c.getJavaDefaultValue();
                        sb.append(", " + defaultValue.replace(";", ""));
                    }
                }
                sb.append(");\n");

                sb.append("value.setCached(Timestamp.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(_CACHED))));\n");
                sb.append("cached.put(id, value);\n");
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
}
