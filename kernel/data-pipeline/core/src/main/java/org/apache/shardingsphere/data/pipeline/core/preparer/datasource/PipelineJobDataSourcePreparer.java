/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.data.pipeline.core.preparer.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.data.pipeline.api.PipelineDataSourceConfiguration;
import org.apache.shardingsphere.data.pipeline.core.datasource.PipelineDataSourceManager;
import org.apache.shardingsphere.data.pipeline.core.metadata.generator.PipelineDDLGenerator;
import org.apache.shardingsphere.data.pipeline.core.preparer.datasource.option.DialectPipelineJobDataSourcePrepareOption;
import org.apache.shardingsphere.data.pipeline.core.preparer.datasource.param.CreateTableConfiguration;
import org.apache.shardingsphere.data.pipeline.core.preparer.datasource.param.PrepareTargetSchemasParameter;
import org.apache.shardingsphere.data.pipeline.core.preparer.datasource.param.PrepareTargetTablesParameter;
import org.apache.shardingsphere.data.pipeline.core.sqlbuilder.sql.PipelinePrepareSQLBuilder;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.database.core.metadata.database.DialectDatabaseMetaData;
import org.apache.shardingsphere.infra.database.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.database.core.type.DatabaseTypeRegistry;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.parser.SQLParserEngine;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Pipeline job data source preparer.
 */
@RequiredArgsConstructor
@Slf4j
public final class PipelineJobDataSourcePreparer {
    
    private static final Pattern PATTERN_CREATE_TABLE_IF_NOT_EXISTS = Pattern.compile("CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern PATTERN_CREATE_TABLE = Pattern.compile("CREATE\\s+TABLE\\s+", Pattern.CASE_INSENSITIVE);
    
    private final DatabaseType databaseType;
    
    /**
     * Prepare target schemas.
     *
     * @param param prepare target schemas parameter
     * @return target schemas
     * @throws SQLException if prepare target schema fail
     */
    public Map<String, ShardingSphereMetaData> prepareTargetSchemas(final PrepareTargetSchemasParameter param) throws SQLException {
        DatabaseType targetDatabaseType = param.getTargetDatabaseType();
        DialectDatabaseMetaData dialectDatabaseMetaData = new DatabaseTypeRegistry(targetDatabaseType).getDialectDatabaseMetaData();
        if (!dialectDatabaseMetaData.isSchemaAvailable()) {
            return Collections.emptyMap();
        }
        Map<String, ShardingSphereMetaData> result = new HashMap<>(param.getCreateTableConfigurations().size(), 1F);
        String defaultSchema = dialectDatabaseMetaData.getDefaultSchema().orElse(null);
        PipelinePrepareSQLBuilder pipelineSQLBuilder = new PipelinePrepareSQLBuilder(targetDatabaseType);
        Collection<String> createdSchemaNames = new HashSet<>(param.getCreateTableConfigurations().size(), 1F);
        for (CreateTableConfiguration each : param.getCreateTableConfigurations()) {
            String targetSchemaName = each.getTargetName().getSchemaName();
            if (null == targetSchemaName || targetSchemaName.equalsIgnoreCase(defaultSchema) || createdSchemaNames.contains(targetSchemaName)) {
                continue;
            }
            Optional<String> sql = pipelineSQLBuilder.buildCreateSchemaSQL(targetSchemaName);
            if (sql.isPresent()) {
                executeCreateSchema(param.getDataSourceManager(), each.getTargetDataSourceConfig(), sql.get()).ifPresent(metaData -> result.put(targetSchemaName, metaData));
                createdSchemaNames.add(targetSchemaName);
            }
        }
        return result;
    }
    
    private Optional<ShardingSphereMetaData> executeCreateSchema(final PipelineDataSourceManager dataSourceManager,
                                                                 final PipelineDataSourceConfiguration targetDataSourceConfig, final String sql) throws SQLException {
        log.info("Prepare target schemas SQL: {}", sql);
        try (
                Connection connection = dataSourceManager.getDataSource(targetDataSourceConfig).getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
            return Optional.of(((ShardingSphereConnection) connection).getContextManager().getMetaDataContexts().getMetaData());
        } catch (final SQLException ex) {
            if (DatabaseTypedSPILoader.findService(DialectPipelineJobDataSourcePrepareOption.class, databaseType)
                    .map(DialectPipelineJobDataSourcePrepareOption::isSupportIfNotExistsOnCreateSchema).orElse(true)) {
                throw ex;
            }
            log.warn("Create schema failed", ex);
        }
        return Optional.empty();
    }
    
    /**
     * Prepare target tables.
     *
     * @param param prepare target tables parameter
     * @throws SQLException SQL exception
     */
    public void prepareTargetTables(final PrepareTargetTablesParameter param) throws SQLException {
        final long startTimeMillis = System.currentTimeMillis();
        PipelineDataSourceManager dataSourceManager = param.getDataSourceManager();
        for (CreateTableConfiguration each : param.getCreateTableConfigurations()) {
            try (Connection targetConnection = dataSourceManager.getDataSource(each.getTargetDataSourceConfig()).getConnection()) {
                ShardingSphereMetaData metaData = param.getTargetSchemaMetaData().get(each.getTargetName().getSchemaName());
                if (null == metaData) {
                    metaData = ((ShardingSphereConnection) targetConnection).getContextManager().getMetaDataContexts().getMetaData();
                }
                List<String> createTargetTableSQL = getCreateTargetTableSQL(each, dataSourceManager, param.getSqlParserEngine(), metaData, param.getTargetDatabaseName());
                for (String sql : createTargetTableSQL) {
                    executeTargetTableSQL(targetConnection, addIfNotExistsForCreateTableSQL(sql));
                }
            }
        }
        log.info("prepareTargetTables cost {} ms", System.currentTimeMillis() - startTimeMillis);
    }
    
    private List<String> getCreateTargetTableSQL(final CreateTableConfiguration createTableConfig, final PipelineDataSourceManager dataSourceManager,
                                                 final SQLParserEngine sqlParserEngine, final ShardingSphereMetaData metaData, final String targetDatabaseName) throws SQLException {
        DatabaseType databaseType = createTableConfig.getSourceDataSourceConfig().getDatabaseType();
        DataSource sourceDataSource = dataSourceManager.getDataSource(createTableConfig.getSourceDataSourceConfig());
        String schemaName = createTableConfig.getSourceName().getSchemaName();
        String sourceTableName = createTableConfig.getSourceName().getTableName();
        String targetTableName = createTableConfig.getTargetName().getTableName();
        return new PipelineDDLGenerator(metaData).generateLogicDDL(databaseType, sourceDataSource, schemaName, sourceTableName, targetTableName, sqlParserEngine, targetDatabaseName);
    }
    
    private void executeTargetTableSQL(final Connection targetConnection, final String sql) throws SQLException {
        log.info("Execute target table SQL: {}", sql);
        try (Statement statement = targetConnection.createStatement()) {
            statement.execute(sql);
        } catch (final SQLException ex) {
            for (String each : DatabaseTypedSPILoader.findService(DialectPipelineJobDataSourcePrepareOption.class, databaseType)
                    .map(DialectPipelineJobDataSourcePrepareOption::getIgnoredExceptionMessages).orElse(Collections.emptyList())) {
                if (ex.getMessage().contains(each)) {
                    return;
                }
            }
            throw ex;
        }
    }
    
    private String addIfNotExistsForCreateTableSQL(final String createTableSQL) {
        return PATTERN_CREATE_TABLE_IF_NOT_EXISTS.matcher(createTableSQL).find() ? createTableSQL : PATTERN_CREATE_TABLE.matcher(createTableSQL).replaceFirst("CREATE TABLE IF NOT EXISTS ");
    }
}
