package com.android305.ddbormg.generators;

import com.android305.ddbormg.mysql.Column;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.android305.ddbormg.mysql.Column.REMARK_NO_CACHE;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class RestGenerator {

    public static String generatePhpCacheController(DatabaseMetaData md, List<String> cachedTables) throws SQLException {
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
                Column col = new Column(c, r);

                if (!col.hasRemark(REMARK_NO_CACHE))
                    columns.add(col.getColumnName());
            }

            String path = "/all/" + c.replace("_", "/");
            sb.append("    /**\n");
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
            sb.append("        return $this->response->withJson($responseArray);\n");
            sb.append("    });\n");
            sb.append('\n');
        }

        sb.append("});");
        return sb.toString();
    }

}
