package io.github.hsseo0501.databasemanager.service;

import io.github.hsseo0501.databasemanager.constant.Constants;
import io.github.hsseo0501.databasemanager.model.Column;
import io.github.hsseo0501.databasemanager.model.PrimaryKey;
import io.github.hsseo0501.databasemanager.model.Procedure;
import io.github.hsseo0501.databasemanager.util.DatabaseUtil;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.*;

@Service
public class MetaCollectServiceImpl implements MetaCollectService {

    @Override
    public List<Column> getColumns(String vendor, String url, String id, String password, String tableName) throws Exception {
        List<Column> columnList = new LinkedList<>();
        DatabaseMetaData databaseMetaData = getDatabaseMetaData(vendor, url, id, password);
        if (vendor.equalsIgnoreCase(Constants.Database.DB_VENDOR_POSTGRESQL)) {
            ResultSet columns = databaseMetaData.getColumns(null, null, tableName, null);

            while (columns.next()) {
                Column column = new Column();
                column.setCatalog(columns.getString(Constants.Database.META_TABLE));
                column.setSchema(columns.getString(Constants.Database.META_TABLE_SCHEMA));
                column.setTableName(columns.getString(Constants.Database.META_TABLE_NAME));
                column.setColumnName(columns.getString(Constants.Database.META_COLUMN_NAME));
                column.setDataType(columns.getString(Constants.Database.META_COLUMN_DATA_TYPE));
                column.setTypeName(columns.getString(Constants.Database.META_COLUMN_TYPE_NAME));
                column.setColumnSize(columns.getInt(Constants.Database.META_COLUMN_SIZE));
                int nullable = columns.getInt(Constants.Database.META_COLUMN_NULLABLE);
                if (nullable == DatabaseMetaData.columnNullable) {
                    column.setNullable("true");
                } else if (nullable == DatabaseMetaData.columnNoNulls) {
                    column.setNullable("false");
                } else {
                    column.setNullable("unknown");
                }
                column.setIsNullable(columns.getString(Constants.Database.META_COLUMN_IS_NULLABLE));
                column.setOrdinalPosition(columns.getString(Constants.Database.META_COLUMN_ORDINAL_POSITION));
                columnList.add(column);
            }
        } else {
            throw new Exception("unknown db vendor");
        }
        return columnList;
    }

    @Override
    public List<PrimaryKey> getPrimaryKeys(String vendor, String url, String id, String password, String tableName) throws Exception {
        List<PrimaryKey> keyList = new LinkedList<>();
        DatabaseMetaData databaseMetaData = getDatabaseMetaData(vendor, url, id, password);
        if (vendor.equalsIgnoreCase(Constants.Database.DB_VENDOR_POSTGRESQL)) {
            ResultSet primaryKeys = databaseMetaData.getPrimaryKeys(null, null, tableName);

            while (primaryKeys.next()) {
                PrimaryKey primaryKey = new PrimaryKey();
                primaryKey.setCatalog(primaryKeys.getString(Constants.Database.META_TABLE));
                primaryKey.setSchema(primaryKeys.getString(Constants.Database.META_TABLE_SCHEMA));
                primaryKey.setTableName(primaryKeys.getString(Constants.Database.META_TABLE_NAME));
                primaryKey.setColumnName(primaryKeys.getString(Constants.Database.META_COLUMN_NAME));
                primaryKey.setKeySequence(primaryKeys.getShort(Constants.Database.META_KEY_SEQUENCE));
                primaryKey.setKeyName(primaryKeys.getString(Constants.Database.META_KEY_NAME));
                keyList.add(primaryKey);
            }
        } else {
            throw new Exception("unknown db vendor");
        }
        return keyList;
    }

    @Override
    public List<String> getSQLKeywords(String vendor, String url, String id, String password, String tableName) throws Exception {
        List<String> keywordList = new LinkedList<>();
        DatabaseMetaData databaseMetaData = getDatabaseMetaData(vendor, url, id, password);
        if (vendor.equalsIgnoreCase(Constants.Database.DB_VENDOR_POSTGRESQL)) {
            String sqlKeywords = databaseMetaData.getSQLKeywords();

            StringTokenizer stringTokenizer = new StringTokenizer(sqlKeywords, ",");
            while (stringTokenizer.hasMoreElements()) {
                keywordList.add(stringTokenizer.nextToken().trim());
            }
        } else {
            throw new Exception("unknown db vendor");
        }
        return keywordList;
    }

    @Override
    public List<Procedure> getStoredProcedures(String vendor, String url, String id, String password, String catalog
            , String schemaPattern, String procedureNamePattern) throws Exception {
        List<Procedure> procedureList = new LinkedList<>();
        DatabaseMetaData databaseMetaData = getDatabaseMetaData(vendor, url, id, password);
        if (vendor.equalsIgnoreCase(Constants.Database.DB_VENDOR_POSTGRESQL)) {
            ResultSet storedProcedures = databaseMetaData.getProcedures(catalog, schemaPattern, procedureNamePattern);

            while (storedProcedures.next()) {
                Procedure procedure = new Procedure();
                procedure.setProcedureName(storedProcedures.getString(Constants.Database.META_PROCEDURE_NAME));
                int storedProcedureType = storedProcedures.getInt(Constants.Database.META_PROCEDURE_TYPE);
                procedure.setProcedureType(getStoredProcedureType(storedProcedureType));
                procedureList.add(procedure);
            }
        } else {
            throw new Exception("unknown db vendor");
        }
        return procedureList;
    }

    private String getStoredProcedureType(int storedProcedureType) {
        if (storedProcedureType == DatabaseMetaData.procedureReturnsResult) {
            return Constants.Database.STORED_PROCEDURE_RETURNS_RESULT;
        } else if (storedProcedureType == DatabaseMetaData.procedureNoResult) {
            return Constants.Database.STORED_PROCEDURE_NO_RESULT;
        } else {
            return Constants.Database.STORED_PROCEDURE_RESULT_UNKNOWN;
        }
    }

    @Override
    public Map<String, String> getTablesAndViews(String vendor, String url, String id, String password
            , Boolean isTable, Boolean isView) throws Exception {
        Map<String, String> tableList = new HashMap<>();
        DatabaseMetaData databaseMetaData = getDatabaseMetaData(vendor, url, id, password);
        if (vendor.equalsIgnoreCase(Constants.Database.DB_VENDOR_POSTGRESQL)) {
            ResultSet tables = null;
            if (isTable && isView) {
                tables = databaseMetaData.getTables(null, null, null, Constants.Database.META_TABLE_AND_VIEW_TYPES);
            } else if (isTable && !isView) {
                tables = databaseMetaData.getTables(null, null, null, Constants.Database.META_TABLE_TYPES);
            } else if (!isTable && isView) {
                tables = databaseMetaData.getTables(null, null, null, Constants.Database.META_VIEW_TYPES);
            }

            while (tables.next()) {
                String name = DatabaseUtil.getTrimmedString(tables, Constants.Database.META_TABLE_NAME);
                String type = DatabaseUtil.getTrimmedString(tables, Constants.Database.META_TABLE_TYPE);
                tableList.put(name, type);
            }
        } else {
            throw new Exception("unknown db vendor");
        }
        return tableList;
    }

    @Override
    public List<String> getTableTypes(String vendor, String url, String id, String password) throws Exception {
        List<String> tableTypeList = new LinkedList<>();
        DatabaseMetaData databaseMetaData = getDatabaseMetaData(vendor, url, id, password);
        if (vendor.equalsIgnoreCase(Constants.Database.DB_VENDOR_POSTGRESQL)) {
            ResultSet tableTypes = databaseMetaData.getTableTypes();

            while (tableTypes.next()) {
                String tableType = tableTypes.getString(1);
                tableTypeList.add(tableType);
            }
        } else {
            throw new Exception("unknown db vendor");
        }
        return tableTypeList;
    }


    @Override
    public List<Column> getBestRowIdentifier(String vendor, String url, String id, String password, String catalog, String schema, String table, Integer scope, Boolean nullable) throws Exception {
        List<Column> columnList = new LinkedList<>();
        DatabaseMetaData databaseMetaData = getDatabaseMetaData(vendor, url, id, password);
        int theScope = DatabaseMetaData.bestRowSession;
        if (scope.equals("bestRowTemporary")) {
            theScope = DatabaseMetaData.bestRowTemporary;
        } else if (scope.equals("bestRowTransaction")) {
            theScope = DatabaseMetaData.bestRowTransaction;
        }
        boolean isNullable = false;

        if (nullable.equals("true")) {
            isNullable = true;
        }

        ResultSet bestRowIdentifier = databaseMetaData.getBestRowIdentifier(catalog, schema, table.toUpperCase(),
                theScope, isNullable);

        while (bestRowIdentifier.next()) {
            Column column = new Column();
            column.setColumnName(bestRowIdentifier.getString(Constants.Database.META_COLUMN_NAME));
            column.setDataType(bestRowIdentifier.getString(Constants.Database.META_COLUMN_DATA_TYPE));
            column.setTypeName(bestRowIdentifier.getString(Constants.Database.META_COLUMN_TYPE_NAME));
            column.setColumnSize(bestRowIdentifier.getInt(Constants.Database.META_COLUMN_SIZE));
            column.setDecimalDigits(bestRowIdentifier.getShort(Constants.Database.META_COLUMN_DECIMAL_DIGITS));
            short pseudoColumn = bestRowIdentifier.getShort(Constants.Database.META_COLUMN_PSEUDO_COLUMN);
            column.setPseudoColumn(getPseudoColumn(pseudoColumn));
            columnList.add(column);
        }

        return columnList;
    }

    private String getScope(short scope) {
        if (scope == DatabaseMetaData.bestRowSession) {
            return "bestRowSession";
        } else if (scope == DatabaseMetaData.bestRowTemporary) {
            return "bestRowTemporary";
        } else if (scope == DatabaseMetaData.bestRowTransaction) {
            return "bestRowTransaction";
        } else {
            return "scope is unknown";
        }
    }


    public String getPseudoColumn(short pseudoColumn) {
        if (pseudoColumn == DatabaseMetaData.bestRowNotPseudo) {
            return "bestRowNotPseudo";
        } else if (pseudoColumn == DatabaseMetaData.bestRowPseudo) {
            return "bestRowPseudo";
        } else {
            return "bestRowUnknown";
        }
    }

    @Override
    public List<String> getCatalogs(String vendor, String url, String id, String password) throws Exception {
        List<String> catalogList = new LinkedList<>();
        DatabaseMetaData databaseMetaData = getDatabaseMetaData(vendor, url, id, password);
        ResultSet catalogs = databaseMetaData.getCatalogs();
        if (vendor.equalsIgnoreCase(Constants.Database.DB_VENDOR_POSTGRESQL)) {
            while (catalogs.next()) {
                String catalog = catalogs.getString(1);
                catalogList.add(catalog);
            }
        } else {
            throw new Exception("unknown db vendor");
        }
        return catalogList;
    }

    @Override
    public List<Column> getColumnPrivileges(String vendor, String url, String id, String password, String catalog, String schema, String table, String columnNamePattern) throws Exception {
        List<Column> columnList = new LinkedList<>();
        DatabaseMetaData databaseMetaData = getDatabaseMetaData(vendor, url, id, password);
        ResultSet columnPrivileges = databaseMetaData.getColumnPrivileges(catalog, schema, table, columnNamePattern);
        while(columnPrivileges.next()) {
            Column column = new Column();
            column.setCatalog(columnPrivileges.getString(Constants.Database.META_TABLE));
            column.setSchema(columnPrivileges.getString(Constants.Database.META_TABLE_SCHEMA));
            column.setTableName(columnPrivileges.getString(Constants.Database.META_TABLE_NAME));
            column.setColumnName(columnPrivileges.getString(Constants.Database.META_COLUMN_NAME));

            column.setGrantor(columnPrivileges.getString(Constants.Database.META_COLUMN_GRANTOR));
            column.setGrantee(columnPrivileges.getString(Constants.Database.META_COLUMN_GRANTEE));
            column.setPrivilege(columnPrivileges.getString(Constants.Database.META_COLUMN_PRIVILEGE));
            column.setIsGrantable(columnPrivileges.getString(Constants.Database.META_COLUMN_IS_GRANTABLE));

            columnList.add(column);
        }

        return columnList;
    }

    @Override
    public ResultSet getExportedKeys(String vendor, String url, String id, String password) throws Exception {
        return null;
    }

    @Override
    public ResultSet getForeignKeys(String vendor, String url, String id, String password, String catalog, String schema, String table) {
        return null;
    }

    @Override
    public String getRowSetMetaData(String vendor, String url, String id, String password, String sqlQuery) throws Exception {
        return null;
    }

    @Override
    public List<String> getSchemas(String vendor, String url, String id, String password) throws Exception {
        return null;
    }

    @Override
    public ResultSet getStoredProcedureColumns(String vendor, String url, String id, String password, String catalog, String schemaPattern, String procedureNamePattern) throws Exception {
        return null;
    }

    @Override
    public ResultSet getTablePrivileges(String vendor, String url, String id, String password, String catalog, String schemaPattern, String tableNamePattern) {
        return null;
    }

    @Override
    public ResultSet getTypes(String vendor, String url, String id, String password) throws Exception {
        return null;
    }

    private DatabaseMetaData getDatabaseMetaData(String vendor, String url, String id, String password) throws Exception {
        DatabaseMetaData databaseMetaData;
        if (vendor.equalsIgnoreCase(Constants.Database.DB_VENDOR_POSTGRESQL)) {
            Class.forName(Constants.Database.JDBC_DRIVER_POSTGRESQL);
            Connection connection = DriverManager.getConnection(url, id, password);
            databaseMetaData = connection.getMetaData();
        } else {
            throw new Exception("unknown db vendor");
        }
        return databaseMetaData;
    }

}