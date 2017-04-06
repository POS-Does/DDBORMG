package com.android305.ddbormg;

import com.android305.ddbormg.generators.AndroidGenerator;
import com.android305.ddbormg.generators.JavaGenerator;
import com.android305.ddbormg.generators.MysqlGenerator;
import com.android305.ddbormg.generators.RestGenerator;
import com.android305.ddbormg.mysql.Table;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
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

    @Parameters(commandDescription = "Generate Android ORM classes")
    private class CommandAndroid {
        @Parameter(names = {"--overwrite", "-o"}, description = "Overwrite regular classes regardless of generated status")
        private boolean overwrite = false;

        @Parameter(names = {"--overwrite-underscore", "-ou"}, description = "Overwrite underscore classes regardless of generated status")
        private boolean overwriteUnderscore = false;
    }

    @Parameters(commandDescription = "Generate PHP restful API cache routes")
    private class CommandRestAPI {
        @Parameter(names = {"--overwrite", "-o"}, description = "Overwrite cache controller regardless of generated status")
        private boolean overwrite = false;
    }

    @Parameters(commandDescription = "Mysql methods")
    private class CommandMysql {
        @Parameter(names = {"--schema", "-s"}, description = "Dump schema")
        private boolean schema = false;

        @Parameter(names = {"--initial", "-i"}, description = "Dump initial data")
        private boolean initial = false;

        @Parameter(names = {"--menu", "-m"}, description = "Dump menu data")
        private boolean menu = false;
    }

    @Parameters(commandDescription = "Generate Java ORM classes")
    private class CommandJava {
        @Parameter(names = {"--package"}, description = "Java main package of app i.e. com.android305.posdoes.print.server", required = true)
        private String packageName;

        @Parameter(names = {"--src-dir"}, description = "Java src directory of app i.e. app/src/", required = true)
        private String srcDirectory;

        @Parameter(names = {"--overwrite", "-o"}, description = "Overwrite regular classes regardless of generated status")
        private boolean overwrite = false;

        @Parameter(names = {"--overwrite-underscore", "-ou"}, description = "Overwrite underscore classes regardless of generated status")
        private boolean overwriteUnderscore = false;
    }

    public static void main(String... args) throws SQLException {
        new Main(args);
    }

    private Connection mConnection;

    @Parameter(names = {"-h", "--host"}, description = "Database host", required = true)
    private String dbHost;

    @Parameter(names = {"--port"}, description = "Database port")
    private String dbPort = "3306";

    @Parameter(names = {"-u", "--username"}, description = "Database username", required = true)
    private String dbUsername;

    @Parameter(names = {"-p", "--password"}, description = "Database password", required = true)
    private String dbPassword;

    private Main(String... args) throws SQLException {
        try {
            JCommander jc = new JCommander(this);
            CommandAndroid android = new CommandAndroid();
            jc.addCommand("android", android);

            CommandRestAPI rest = new CommandRestAPI();
            jc.addCommand("rest", rest);

            CommandMysql mysql = new CommandMysql();
            jc.addCommand("mysql", mysql);

            CommandJava java = new CommandJava();
            jc.addCommand("java", java);

            jc.parse(args);

            mConnection = getConnection();

            if (jc.getParsedCommand() == null) {
                System.err.println("Requires one command android, rest, mysql, java");
                System.exit(1);
            }

            switch (jc.getParsedCommand()) {
                case "android":
                    doAndroid(android.overwrite, android.overwriteUnderscore);
                    break;
                case "rest":
                    doRest(rest.overwrite);
                    break;
                case "mysql":
                    doMysql(mysql.schema, mysql.initial, mysql.menu);
                    break;
                case "java":
                    doJava(java.srcDirectory, java.packageName, java.overwrite, java.overwriteUnderscore);
                    break;
                default:
                    System.err.println("Requires one command android, rest, mysql, java");
                    System.exit(1);
            }
        } catch (ParameterException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    private void doAndroid(boolean overwrite, boolean overwriteUnderscore) throws SQLException {
        DatabaseMetaData md = mConnection.getMetaData();
        ArrayList<String> cacheTables = new ArrayList<>();
        ResultSet result = md.getTables(null, null, null, null);

        while (result.next()) {
            String tableName = result.getString("TABLE_NAME");
            String remarks = result.getString("REMARKS");
            boolean cached = remarks != null && remarks.contains("cache");
            boolean skip = remarks != null && remarks.contains("no_api");
            if (!skip) {
                if (cached) {
                    cacheTables.add(tableName);
                }
                String className = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, tableName);
                File dir = new File("app/src/main/java/com/android305/posdoes/rest/objects");
                File javaFile = new File(dir, className + ".java");
                File underscoreFile = new File(dir, className + "_.java");
                System.out.println("Generating Class `" + tableName + "`...");
                generateFile(javaFile, AndroidGenerator.generateClass(md, tableName, className, cached), false, overwrite);
                System.out.println("Generating Class `" + tableName + "_`...");
                generateFile(underscoreFile, AndroidGenerator.generateUnderscoreClass(md, tableName, className, cached), true, overwriteUnderscore);
            }
        }
        System.out.println("Generating Cache Controller...");
        File dir = new File("app/src/main/java/com/android305/posdoes/rest/routes");
        File cacheFile = new File(dir, "CacheController.java");
        generateFile(cacheFile, AndroidGenerator.generateControllerClass(cacheTables), false, overwrite);

        System.out.println("Generating Sqlite Class...");
        dir = new File("app/src/main/java/com/android305/posdoes/service/sqlite");
        File sqliteFile = new File(dir, "POSDbHelper.java");
        generateFile(sqliteFile, AndroidGenerator.generateSqliteClass(cacheTables), false, overwrite);
    }

    private void doRest(boolean overwrite) throws SQLException {
        DatabaseMetaData md = mConnection.getMetaData();
        ArrayList<String> cacheTables = new ArrayList<>();
        ResultSet result = md.getTables(null, null, null, null);
        while (result.next()) {
            String tableName = result.getString("TABLE_NAME");
            String remarks = result.getString("REMARKS");
            if (remarks != null && remarks.contains("cache")) {
                cacheTables.add(tableName);
            }
        }
        File dir = new File("src/v1/routes/");
        File cacheFile = new File(dir, "cache.php");

        generateFile(cacheFile, RestGenerator.generatePhpCacheController(md, cacheTables), false, overwrite);
    }

    private void doMysql(boolean schema, boolean initial, boolean menu) throws SQLException {
        DatabaseMetaData md = mConnection.getMetaData();
        ResultSet result = md.getTables(null, null, null, null);
        ArrayList<Table> tables = new ArrayList<>();
        while (result.next()) {
            String tableName = result.getString("TABLE_NAME");
            String remarks = result.getString("REMARKS");
            Table t = new Table(tableName, remarks);
            System.out.println("Loading table `" + t.getName() + "`");
            t.loadColumns(md);
            t.loadForeignKeys(md);
            t.loadIndexes(md);
            tables.add(t);
        }

        if (schema) {
            System.out.println("Generating schema...");
            try {
                File dir = new File("db_schema");
                File schemaFile = new File(dir, "schema.sql");
                FileUtils.writeStringToFile(schemaFile, MysqlGenerator.generateSchema(tables), Charset.forName("UTF-8"));
                System.out.println("Schema written to db_schema/schema.sql");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (initial) {
            System.out.println("Generating initial data...");
            try {
                File dir = new File("db_schema");
                File schemaFile = new File(dir, "initial_data.sql");
                FileUtils.writeStringToFile(schemaFile, MysqlGenerator.generateInitialData(tables, mConnection), Charset.forName("UTF-8"));
                System.out.println("Schema written to db_schema/initial_data.sql");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (menu) {
            System.out.println("Generating menu data...");
            try {
                File dir = new File("db_schema");
                File schemaFile = new File(dir, "menu_data.sql");
                FileUtils.writeStringToFile(schemaFile, MysqlGenerator.generateMenuData(tables, mConnection), Charset.forName("UTF-8"));
                System.out.println("Schema written to db_schema/menu_data.sql");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void doJava(String srcDirectory, String packageName, boolean overwrite, boolean overwriteUnderscore) throws SQLException {
        DatabaseMetaData md = mConnection.getMetaData();
        ResultSet result = md.getTables(null, null, null, null);

        while (result.next()) {
            String tableName = result.getString("TABLE_NAME");
            String remarks = result.getString("REMARKS");
            boolean skip = remarks != null && remarks.contains("no_api");
            if (!skip) {
                String className = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, tableName);
                File dir = new File(srcDirectory, "main/java/ " + packageName.replaceAll("\\.", "\\/").trim() + "/rest/objects");
                File javaFile = new File(dir, className + ".java");
                File underscoreFile = new File(dir, className + "_.java");
                System.out.println("Generating Class `" + tableName + "`...");
                generateFile(javaFile, JavaGenerator.generateClass(packageName, md, tableName, className), false, overwrite);
                System.out.println("Generating Class `" + tableName + "_`...");
                generateFile(underscoreFile, JavaGenerator.generateUnderscoreClass(packageName, md, tableName, className), true, overwriteUnderscore);
            }
        }
    }

    private void generateFile(File javaFile, String java, boolean ignore, boolean overwrite) {
        boolean write = overwrite;
        if (!write) {
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

    private Connection getConnection() throws SQLException {
        Properties connectionProps = new Properties();
        connectionProps.put("user", dbUsername);
        connectionProps.put("password", dbPassword);
        connectionProps.setProperty("useInformationSchema", "true");
        connectionProps.setProperty("useSSL", "false");
        return DriverManager.getConnection("jdbc:mysql://" + dbHost + ":" + dbPort + "/posdb", connectionProps);
    }
}