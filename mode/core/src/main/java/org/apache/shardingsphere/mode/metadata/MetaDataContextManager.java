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

package org.apache.shardingsphere.mode.metadata;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.infra.datasource.pool.props.domain.DataSourcePoolProperties;
import org.apache.shardingsphere.infra.instance.ComputeNodeInstanceContext;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.rule.RuleMetaData;
import org.apache.shardingsphere.infra.metadata.database.schema.manager.GenericSchemaManager;
import org.apache.shardingsphere.infra.metadata.statistics.builder.ShardingSphereStatisticsFactory;
import org.apache.shardingsphere.infra.rule.builder.global.GlobalRulesBuilder;
import org.apache.shardingsphere.mode.metadata.factory.MetaDataContextsFactory;
import org.apache.shardingsphere.mode.metadata.manager.DatabaseRuleConfigurationManager;
import org.apache.shardingsphere.mode.metadata.manager.GlobalConfigurationManager;
import org.apache.shardingsphere.mode.metadata.manager.ResourceSwitchManager;
import org.apache.shardingsphere.mode.metadata.manager.RuleItemManager;
import org.apache.shardingsphere.mode.metadata.manager.DatabaseMetaDataManager;
import org.apache.shardingsphere.mode.metadata.manager.StatisticsManager;
import org.apache.shardingsphere.mode.metadata.manager.StorageUnitManager;
import org.apache.shardingsphere.mode.metadata.manager.SwitchingResource;
import org.apache.shardingsphere.mode.metadata.persist.MetaDataPersistFacade;
import org.apache.shardingsphere.mode.spi.repository.PersistRepository;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * Meta data context manager.
 */
@Getter
@Slf4j
public class MetaDataContextManager {
    
    private final MetaDataContexts metaDataContexts;
    
    private final ComputeNodeInstanceContext computeNodeInstanceContext;
    
    private final MetaDataPersistFacade metaDataPersistFacade;
    
    private final StatisticsManager statisticsManager;
    
    private final DatabaseMetaDataManager databaseMetaDataManager;
    
    private final RuleItemManager ruleItemManager;
    
    private final ResourceSwitchManager resourceSwitchManager;
    
    private final StorageUnitManager storageUnitManager;
    
    private final DatabaseRuleConfigurationManager databaseRuleConfigurationManager;
    
    private final GlobalConfigurationManager globalConfigurationManager;
    
    public MetaDataContextManager(final MetaDataContexts metaDataContexts, final ComputeNodeInstanceContext computeNodeInstanceContext, final PersistRepository repository) {
        this.metaDataContexts = metaDataContexts;
        this.computeNodeInstanceContext = computeNodeInstanceContext;
        metaDataPersistFacade = new MetaDataPersistFacade(repository);
        resourceSwitchManager = new ResourceSwitchManager();
        statisticsManager = new StatisticsManager(metaDataContexts);
        storageUnitManager = new StorageUnitManager(metaDataContexts, computeNodeInstanceContext, resourceSwitchManager, metaDataPersistFacade);
        databaseRuleConfigurationManager = new DatabaseRuleConfigurationManager(metaDataContexts, computeNodeInstanceContext, metaDataPersistFacade);
        databaseMetaDataManager = new DatabaseMetaDataManager(metaDataContexts, metaDataPersistFacade);
        ruleItemManager = new RuleItemManager(metaDataContexts, databaseRuleConfigurationManager, metaDataPersistFacade);
        globalConfigurationManager = new GlobalConfigurationManager(metaDataContexts, metaDataPersistFacade);
    }
    
    /**
     * Refresh database meta data.
     *
     * @param database to be reloaded database
     */
    public void refreshDatabaseMetaData(final ShardingSphereDatabase database) {
        try {
            MetaDataContexts reloadedMetaDataContexts = createMetaDataContexts(database);
            dropSchemas(database.getName(), reloadedMetaDataContexts.getMetaData().getDatabase(database.getName()), database);
            metaDataContexts.update(reloadedMetaDataContexts);
            metaDataContexts.getMetaData().getDatabase(database.getName()).getAllSchemas()
                    .forEach(each -> metaDataPersistFacade.getDatabaseMetaDataFacade().getSchema().alterByRefresh(database.getName(), each));
        } catch (final SQLException ex) {
            log.error("Refresh database meta data: {} failed", database.getName(), ex);
        }
    }
    
    private MetaDataContexts createMetaDataContexts(final ShardingSphereDatabase database) throws SQLException {
        Map<String, DataSourcePoolProperties> dataSourcePoolProps = metaDataPersistFacade.getDataSourceUnitService().load(database.getName());
        SwitchingResource switchingResource = resourceSwitchManager.switchByAlterStorageUnit(database.getResourceMetaData(), dataSourcePoolProps);
        Collection<RuleConfiguration> ruleConfigs = metaDataPersistFacade.getDatabaseRuleService().load(database.getName());
        ShardingSphereDatabase changedDatabase = new MetaDataContextsFactory(metaDataPersistFacade, computeNodeInstanceContext)
                .createChangedDatabase(database.getName(), false, switchingResource, ruleConfigs, metaDataContexts);
        metaDataContexts.getMetaData().putDatabase(changedDatabase);
        ConfigurationProperties props = new ConfigurationProperties(metaDataPersistFacade.getPropsService().load());
        Collection<RuleConfiguration> globalRuleConfigs = metaDataPersistFacade.getGlobalRuleService().load();
        RuleMetaData changedGlobalMetaData = new RuleMetaData(GlobalRulesBuilder.buildRules(globalRuleConfigs, metaDataContexts.getMetaData().getAllDatabases(), props));
        ShardingSphereMetaData metaData = new ShardingSphereMetaData(
                metaDataContexts.getMetaData().getAllDatabases(), metaDataContexts.getMetaData().getGlobalResourceMetaData(), changedGlobalMetaData, props);
        MetaDataContexts result = new MetaDataContexts(metaData, ShardingSphereStatisticsFactory.create(metaData, metaDataPersistFacade.getStatisticsService().load(metaData)));
        switchingResource.closeStaleDataSources();
        return result;
    }
    
    private void dropSchemas(final String databaseName, final ShardingSphereDatabase reloadDatabase, final ShardingSphereDatabase currentDatabase) {
        GenericSchemaManager.getToBeDroppedSchemaNames(reloadDatabase, currentDatabase).forEach(each -> metaDataPersistFacade.getDatabaseMetaDataFacade().getSchema().drop(databaseName, each));
    }
}
