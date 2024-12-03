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

package org.apache.shardingsphere.infra.metadata.statistics.builder.dialect;

import org.apache.shardingsphere.infra.autogen.version.ShardingSphereVersion;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereSchema;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereTable;
import org.apache.shardingsphere.infra.metadata.statistics.ShardingSphereDatabaseData;
import org.apache.shardingsphere.infra.metadata.statistics.ShardingSphereRowData;
import org.apache.shardingsphere.infra.metadata.statistics.ShardingSphereSchemaData;
import org.apache.shardingsphere.infra.metadata.statistics.ShardingSphereStatistics;
import org.apache.shardingsphere.infra.metadata.statistics.ShardingSphereTableData;
import org.apache.shardingsphere.infra.metadata.statistics.builder.ShardingSphereStatisticsBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * ShardingSphere statistics builder for PostgreSQL.
 */

public final class PostgreSQLShardingSphereStatisticsBuilder implements ShardingSphereStatisticsBuilder {
    
    private static final String SHARDINGSPHERE = "shardingsphere";
    
    private static final String CLUSTER_INFORMATION = "cluster_information";
    
    private static final String SHARDING_TABLE_STATISTICS = "sharding_table_statistics";
    
    private static final Map<String, Collection<String>> INIT_DATA_SCHEMA_TABLES = new LinkedHashMap<>();
    
    static {
        INIT_DATA_SCHEMA_TABLES.put("pg_catalog", Arrays.asList("pg_class", "pg_namespace"));
    }
    
    @Override
    public ShardingSphereStatistics build(final ShardingSphereMetaData metaData) {
        ShardingSphereStatistics result = new ShardingSphereStatistics();
        for (ShardingSphereDatabase each : metaData.getAllDatabases()) {
            ShardingSphereDatabaseData databaseData = new ShardingSphereDatabaseData();
            initSchemas(each, databaseData);
            if (!databaseData.getSchemaData().isEmpty()) {
                result.putDatabase(each.getName(), databaseData);
            }
        }
        return result;
    }
    
    private void initSchemas(final ShardingSphereDatabase database, final ShardingSphereDatabaseData databaseData) {
        if (null != database.getSchema(SHARDINGSPHERE)) {
            ShardingSphereSchemaData schemaData = new ShardingSphereSchemaData();
            initClusterInformationTable(schemaData);
            initShardingTableStatisticsTable(schemaData);
            databaseData.putSchema(SHARDINGSPHERE, schemaData);
        }
        for (Entry<String, Collection<String>> entry : INIT_DATA_SCHEMA_TABLES.entrySet()) {
            ShardingSphereSchemaData schemaData = new ShardingSphereSchemaData();
            if (null != database.getSchema(entry.getKey())) {
                initTables(database.getSchema(entry.getKey()), entry.getValue(), schemaData);
                databaseData.putSchema(entry.getKey(), schemaData);
            }
        }
    }
    
    private void initClusterInformationTable(final ShardingSphereSchemaData schemaData) {
        ShardingSphereTableData tableData = new ShardingSphereTableData(CLUSTER_INFORMATION);
        tableData.getRows().add(new ShardingSphereRowData(Collections.singletonList(ShardingSphereVersion.VERSION)));
        schemaData.putTable(CLUSTER_INFORMATION, tableData);
    }
    
    private void initShardingTableStatisticsTable(final ShardingSphereSchemaData schemaData) {
        schemaData.putTable(SHARDING_TABLE_STATISTICS, new ShardingSphereTableData(SHARDING_TABLE_STATISTICS));
    }
    
    private void initTables(final ShardingSphereSchema schema, final Collection<String> tables, final ShardingSphereSchemaData schemaData) {
        for (ShardingSphereTable each : schema.getAllTables()) {
            if (tables.contains(each.getName().toLowerCase())) {
                schemaData.putTable(each.getName().toLowerCase(), new ShardingSphereTableData(each.getName()));
            }
        }
    }
    
    @Override
    public String getDatabaseType() {
        return "PostgreSQL";
    }
}
