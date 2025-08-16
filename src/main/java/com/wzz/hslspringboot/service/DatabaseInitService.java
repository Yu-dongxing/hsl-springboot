package com.wzz.hslspringboot.service;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wzz.hslspringboot.annotation.ColumnType;
import com.wzz.hslspringboot.annotation.DefaultValue; // ▼▼▼ 新增导入 ▼▼▼
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseInitService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void initDatabase() throws SQLException {
        List<Class<?>> entityClasses = scanEntityClasses("com.wzz.hslspringboot.pojo");

        for (Class<?> entityClass : entityClasses) {
            TableName tableNameAnnotation = entityClass.getAnnotation(TableName.class);
            if (tableNameAnnotation != null) {
                String tableName = tableNameAnnotation.value();
                createOrUpdateTable(entityClass, tableName);
            }
        }
    }

    private List<Class<?>> scanEntityClasses(String basePackage) {
        List<Class<?>> entityClasses = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(TableName.class));

        for (var beanDef : scanner.findCandidateComponents(basePackage)) {
            try {
                Class<?> entityClass = Class.forName(beanDef.getBeanClassName());
                entityClasses.add(entityClass);
            } catch (ClassNotFoundException e) {
                System.err.println("Class not found: " + beanDef.getBeanClassName());
                e.printStackTrace();
            }
        }
        return entityClasses;
    }

    private boolean tableExists(String tableName) throws SQLException {
        try {
            DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (Exception e) {
            System.err.println("Error checking if table " + tableName + " exists: " + e.getMessage());
            throw e;
        }
    }

    private void createOrUpdateTable(Class<?> entityClass, String tableName) throws SQLException {
        if (!tableExists(tableName)) {
            createTable(entityClass, tableName);
        } else {
            updateTable(entityClass, tableName);
        }
    }

    private void createTable(Class<?> entityClass, String tableName) throws SQLException {
        try {
            StringBuilder createTableSQL = new StringBuilder("CREATE TABLE ");
            createTableSQL.append(tableName).append(" (");

            Field[] fields = entityClass.getDeclaredFields();
            List<String> fieldDefinitions = new ArrayList<>();
            boolean hasPrimaryKey = false;

            for (Field field : fields) {
                TableId tableId = field.getAnnotation(TableId.class);
                TableField tableField = field.getAnnotation(TableField.class);

                String columnName;
                String dataType;
                String defaultValueSql = getDefaultValueSql(field); // ▼▼▼ 新增代码：获取默认值SQL ▼▼▼

                if (tableId != null) {
                    columnName = tableId.value().isEmpty() ? field.getName() : tableId.value();
                    dataType = getDataType(field);
                    // ▼▼▼ 修改后的行：追加默认值SQL ▼▼▼
                    fieldDefinitions.add(columnName + " " + dataType + " PRIMARY KEY AUTO_INCREMENT" + defaultValueSql);
                    hasPrimaryKey = true;
                } else if (tableField != null) {
                    columnName = tableField.value().isEmpty() ? field.getName() : tableField.value();
                    dataType = getDataType(field);
                    // ▼▼▼ 修改后的行：追加默认值SQL ▼▼▼
                    fieldDefinitions.add(columnName + " " + dataType + defaultValueSql);
                }
            }

            if (!hasPrimaryKey) {
                fieldDefinitions.add("id BIGINT PRIMARY KEY AUTO_INCREMENT");
            }

            createTableSQL.append(String.join(", ", fieldDefinitions)).append(");");
            jdbcTemplate.execute(createTableSQL.toString());
            System.out.println("Created table " + tableName);
        } catch (Exception e) {
            System.err.println("Error creating table " + tableName + ": " + e.getMessage());
            throw e;
        }
    }

    private void updateTable(Class<?> entityClass, String tableName) throws SQLException {
        try {
            Field[] fields = entityClass.getDeclaredFields();
            for (Field field : fields) {
                TableField tableField = field.getAnnotation(TableField.class);
                if (tableField != null) {
                    String columnName = tableField.value().isEmpty() ? field.getName() : tableField.value();
                    if (!columnExists(tableName, columnName)) {
                        String dataType = getDataType(field);
                        String defaultValueSql = getDefaultValueSql(field); // 获取默认值SQL
                        //  修改后的行：追加默认值SQL
                        String addColumnSQL = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + dataType + defaultValueSql + ";";
                        try {
                            jdbcTemplate.execute(addColumnSQL);
                            System.out.println("Added column " + columnName + " to table " + tableName);
                        } catch (Exception e) {
                            System.err.println("Failed to add column " + columnName + " to table " + tableName + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating table " + tableName + ": " + e.getMessage());
            throw e;
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        try {
            DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
                return rs.next();
            }
        } catch (Exception e) {
            System.err.println("Error checking if column " + columnName + " exists in table " + tableName + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * 检查字段上是否有 @DefaultValue 注解，并返回对应的 SQL "DEFAULT" 子句。
     * @param field 要检查的字段
     * @return 如果注解存在，返回 " DEFAULT ... "，否则返回空字符串。
     */
    private String getDefaultValueSql(Field field) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            DefaultValue defaultValueAnnotation = field.getAnnotation(DefaultValue.class);
            return " DEFAULT " + defaultValueAnnotation.value();
        }
        return ""; // 如果没有注解，返回空字符串
    }


    private String getDataType(Field field) {
        // 优先检查我们自定义的 @ColumnType 注解
        if (field.isAnnotationPresent(ColumnType.class)) {
            ColumnType columnType = field.getAnnotation(ColumnType.class);
            return columnType.value(); // 如果有注解，直接返回注解中定义的值，例如 "MEDIUMTEXT"
        }

        // 如果没有注解，则执行原有的默认映射逻辑
        Class<?> fieldType = field.getType();
        if (fieldType.equals(String.class)) {
            return "VARCHAR(255)";
        } else if (fieldType.equals(LocalDateTime.class)) {
            return "DATETIME";
        } else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
            return "BIGINT";
        } else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
            return "INT";
        } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
            return "DOUBLE";
        } else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
            return "FLOAT";
        } else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
            return "TINYINT";
        } else if (fieldType.equals(Date.class)) {
            return "DATETIME";
        } else if (fieldType.equals(java.sql.Timestamp.class)) {
            return "TIMESTAMP";
        } else if (fieldType.equals(BigDecimal.class)) {
            return "DECIMAL(19,2)";
        } else if (fieldType.equals(Byte.class) || fieldType.equals(byte.class)) {
            return "TINYINT";
        } else if (fieldType.equals(Short.class) || fieldType.equals(short.class)) {
            return "SMALLINT";
        } else if (Collection.class.isAssignableFrom(fieldType)) {
            return "JSON";
        } else if (Map.class.isAssignableFrom(fieldType)) {
            return "JSON";
        } else {
            // 默认回退选项
            return "VARCHAR(255)";
        }
    }
}