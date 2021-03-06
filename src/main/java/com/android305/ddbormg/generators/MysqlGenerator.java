package com.android305.ddbormg.generators;

import com.android305.ddbormg.mysql.Column;
import com.android305.ddbormg.mysql.ForeignKey;
import com.android305.ddbormg.mysql.Index;
import com.android305.ddbormg.mysql.Table;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings({"StringConcatenationInsideStringBufferAppend", "StringBufferReplaceableByString"})
public class MysqlGenerator {

    private static String getVersion() {
        Package p = MysqlGenerator.class.getPackage();
        String version = "dev";
        if (p != null) {
            version = p.getImplementationVersion();
            if (version == null) {
                return "dev";
            }
        }
        return version;
    }

    public static String generateSchema(ArrayList<Table> tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Generated by DDBORMG Version: " + getVersion() + "\n\n");

        sb.append("SET SQL_MODE = \"NO_AUTO_VALUE_ON_ZERO\";\n");
        sb.append("SET time_zone = \"+00:00\";\n\n");

        sb.append("/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;\n");
        sb.append("/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;\n");
        sb.append("/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;\n");
        sb.append("/*!40101 SET NAMES utf8mb4 */;\n\n");

        sb.append("--\n");
        sb.append("-- Database: `posdb`\n");
        sb.append("--\n\n");

        sb.append("CREATE DATABASE IF NOT EXISTS `posdb` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n");
        sb.append("USE `posdb`;\n\n");

        for (Table t : tables) {
            sb.append("--\n");
            sb.append("-- Table structure for table `" + t.getName() + "`\n");
            sb.append("--\n");
            sb.append("CREATE TABLE `" + t.getName() + "` (\n");
            sb.append("  `ID` int(11) NOT NULL AUTO_INCREMENT,\n");
            sb.append("  `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,\n");
            sb.append("  `MODIFIED_TIME` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,\n");
            for (Column c : t.getColumns().values()) {
                if (!c.getColumnName().equals("ID") && !c.getColumnName().equals("CREATED_TIME") && !c.getColumnName().equals("MODIFIED_TIME")) {
                    sb.append("  `" + c.getColumnName() + "` " + c.getColumnType());

                    switch (c.getColumnType()) {
                        case "JSON":
                        case "TIMESTAMP":
                        case "DATE":
                        case "TIME":
                        case "DOUBLE":
                        case "DATETIME":
                        case "TEXT":
                            sb.append(" ");
                            break;
                        case "TINYINT":
                        case "BIT":
                            sb.append("(1) ");
                            break;
                        case "INT":
                            sb.append("(11) ");
                            break;
                        default:
                            sb.append("(" + c.getColumnSize() + ") ");
                            break;
                    }

                    if (c.getColumnType().equals("VARCHAR")) {
                        sb.append("COLLATE utf8mb4_unicode_ci ");
                    }
                    if (!c.nullable()) {
                        sb.append("NOT NULL ");
                    } else if (c.getDefaultValue() == null) {
                        sb.append("DEFAULT NULL ");
                    }
                    if (c.getDefaultValue() != null) {
                        sb.append("DEFAULT '" + c.getDefaultValue() + "' ");
                    }
                    if (c.getRawRemarks() != null && !c.getRawRemarks().equals("")) {
                        sb.append("COMMENT '" + c.getRawRemarks() + "' ");
                    }

                    int lastSpace = sb.toString().lastIndexOf(" ");
                    sb.replace(lastSpace, lastSpace + 1, ",\n");
                }
            }
            sb.append("  PRIMARY KEY (`ID`)\n");
            if (t.getRawRemarks() != null && !t.getRawRemarks().equals("")) {
                sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='" + t.getRawRemarks() + "' ROW_FORMAT=DYNAMIC;\n\n");
            } else {
                sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;\n\n");
            }
        }

        sb.append("--\n");
        sb.append("-- Indexes for dumped tables\n");
        sb.append("--\n\n");

        for (Table t : tables) {
            Collection<Index> indexes = t.getIndexes().values();
            if (indexes.size() > 0) {
                sb.append("--\n");
                sb.append("-- Indexes for table `" + t.getName() + "`\n");
                sb.append("--\n");
                sb.append("ALTER TABLE `" + t.getName() + "`\n");
                for (Index i : indexes) {
                    sb.append("  ADD ");
                    if (i.isUnique()) {
                        sb.append("UNIQUE ");
                    }
                    sb.append("KEY `" + i.getName() + "` (");

                    for (String column : i.getColumns()) {
                        sb.append("`" + column + "`,");
                    }
                    int lastComma = sb.toString().lastIndexOf(",");
                    sb.replace(lastComma, lastComma + 1, "),\n");
                }

                int lastComma = sb.toString().lastIndexOf(",");
                sb.replace(lastComma, lastComma + 1, ";\n");
            }
        }

        sb.append("--\n");
        sb.append("-- Constraints for dumped tables\n");
        sb.append("--\n\n");

        for (Table t : tables) {
            Collection<ForeignKey> foreignKeys = t.getForeignKeys().values();
            if (foreignKeys.size() > 0) {
                sb.append("--\n");
                sb.append("-- Constraints for table `" + t.getName() + "`\n");
                sb.append("--\n");
                sb.append("ALTER TABLE `" + t.getName() + "`\n");
                for (ForeignKey fk : foreignKeys) {
                    sb.append("  ADD CONSTRAINT `" + fk.getName() + "` FOREIGN KEY (`" + fk.getColumnName() + "`) REFERENCES `" + fk.getReferencesTableName() + "` (`" + fk.getReferencesColumnName() + "`),\n");
                }

                int lastComma = sb.toString().lastIndexOf(",");
                sb.replace(lastComma, lastComma + 1, ";\n");
            }
        }

        sb.append("/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;\n");
        sb.append("/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;\n");
        sb.append("/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;\n");
        return sb.toString();
    }

    public static String generateInitialData(ArrayList<Table> tables, Connection connection) throws SQLException {
        Table company = null;
        Table permission = null;
        Table user = null;
        Table userType = null;

        for (Table t : tables) {
            switch (t.getName()) {
                case "company":
                    company = t;
                    break;
                case "permission":
                    permission = t;
                    break;
                case "user":
                    user = t;
                    break;
                case "user_type":
                    userType = t;
                    break;
            }
        }

        assert company != null;
        assert permission != null;
        assert user != null;
        assert userType != null;
        StringBuilder sb = new StringBuilder();
        sb.append("-- Generated by DDBORMG Version: " + getVersion() + "\n\n");

        sb.append("SET SQL_MODE = \"NO_AUTO_VALUE_ON_ZERO\";\n");
        sb.append("SET time_zone = \"+00:00\";\n\n");

        sb.append("/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;\n");
        sb.append("/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;\n");
        sb.append("/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;\n");
        sb.append("/*!40101 SET NAMES utf8mb4 */;\n\n");

        sb.append("--\n");
        sb.append("-- Database: `posdb`\n");
        sb.append("--\n\n");

        sb.append("USE `posdb`;\n\n");

        sb.append(truncateTables(company, permission, userType, user));

        sb.append(generateInsert(company, connection, "WHERE ID = 1"));
        sb.append(generateInsert(permission, connection, null));
        sb.append(generateInsert(userType, connection, "WHERE ID = 1 OR ID = 2"));
        sb.append(generateInsert(user, connection, "WHERE ID = 1"));

        sb.append("/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;\n");
        sb.append("/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;\n");
        sb.append("/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;\n");
        return sb.toString();
    }

    public static String generateMenuData(ArrayList<Table> tables, Connection connection) throws SQLException {
        Table majorCategory = null;
        Table menuCategory = null;
        Table menuModifierGroup = null;
        Table menuModifierPage = null;
        Table menuModifierPageByGroup = null;
        Table menuModifier = null;
        Table menuItem = null;
        Table menuItemGroup = null;

        for (Table t : tables) {
            switch (t.getName()) {
                case "major_category":
                    majorCategory = t;
                    break;
                case "menu_category":
                    menuCategory = t;
                    break;
                case "menu_modifier_group":
                    menuModifierGroup = t;
                    break;
                case "menu_modifier_page":
                    menuModifierPage = t;
                    break;
                case "menu_modifier_page_by_group":
                    menuModifierPageByGroup = t;
                    break;
                case "menu_modifier":
                    menuModifier = t;
                    break;
                case "menu_item":
                    menuItem = t;
                    break;
                case "menu_item_group":
                    menuItemGroup = t;
                    break;
            }
        }

        assert majorCategory != null;
        assert menuCategory != null;
        assert menuModifierGroup != null;
        assert menuModifierPage != null;
        assert menuModifierPageByGroup != null;
        assert menuModifier != null;
        assert menuItem != null;
        assert menuItemGroup != null;
        StringBuilder sb = new StringBuilder();
        sb.append("-- Generated by DDBORMG Version: " + getVersion() + "\n\n");

        sb.append("SET SQL_MODE = \"NO_AUTO_VALUE_ON_ZERO\";\n");
        sb.append("SET time_zone = \"+00:00\";\n\n");

        sb.append("/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;\n");
        sb.append("/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;\n");
        sb.append("/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;\n");
        sb.append("/*!40101 SET NAMES utf8mb4 */;\n\n");

        sb.append("--\n");
        sb.append("-- Database: `posdb`\n");
        sb.append("--\n\n");

        sb.append("USE `posdb`;\n\n");

        sb.append(truncateTables(majorCategory, menuCategory, menuModifierGroup, menuModifierPage, menuModifierPageByGroup, menuModifier, menuItemGroup, menuItem));

        sb.append(generateInsert(majorCategory, connection, null));
        sb.append(generateInsert(menuCategory, connection, null));
        sb.append(generateInsert(menuModifierGroup, connection, null));
        sb.append(generateInsert(menuModifierPage, connection, null));
        sb.append(generateInsert(menuModifierPageByGroup, connection, null));
        sb.append(generateInsert(menuModifier, connection, null));
        sb.append(generateInsert(menuItemGroup, connection, null));
        sb.append(generateInsert(menuItem, connection, null));

        sb.append("/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;\n");
        sb.append("/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;\n");
        sb.append("/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;\n");
        return sb.toString();
    }

    private static String truncateTables(Table... tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET FOREIGN_KEY_CHECKS=0;\n");
        for (Table table : tables) {
            sb.append("TRUNCATE TABLE `" + table.getName() + "`;\n");
        }
        sb.append("SET FOREIGN_KEY_CHECKS=1;\n\n");
        return sb.toString();
    }

    private static String generateInsert(Table table, Connection connection, String where) throws SQLException {
        StringBuilder sb = new StringBuilder();

        sb.append("INSERT INTO `" + table.getName() + "` (");

        for (Column c : table.getColumns().values()) {
            if (c.insertDetail()) {
                sb.append("`" + c.getColumnName() + "`,");
            }
        }
        int lastComma = sb.toString().lastIndexOf(",");
        sb.replace(lastComma, lastComma + 1, ") VALUES \n");

        Statement st = connection.createStatement();
        String sql = ("SELECT * FROM `" + table.getName() + "`");
        if (where != null) {
            sql += " " + where;
        }
        sql += ";";
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            sb.append("(");
            for (Column c : table.getColumns().values()) {
                if (c.insertDetail()) {
                    switch (c.getColumnType()) {
                        case "INT":
                        case "TINYINT":
                        case "BIT":
                            sb.append(rs.getString(c.getColumnName()) + ",");
                            break;
                        default:
                            String value = rs.getString(c.getColumnName());
                            if (value == null) {
                                sb.append("NULL,");
                            } else {
                                sb.append("'" + value.replace("'", "\\\'") + "',");
                            }
                            break;
                    }
                }
            }
            lastComma = sb.toString().lastIndexOf(",");
            sb.replace(lastComma, lastComma + 1, "),\n");
        }

        lastComma = sb.toString().lastIndexOf(",");
        sb.replace(lastComma, lastComma + 1, ";\n");
        return sb.toString();
    }
}
