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

package org.apache.shardingsphere.sharding.checker.sql.ddl;

import org.apache.shardingsphere.infra.binder.context.statement.ddl.AlterIndexStatementContext;
import org.apache.shardingsphere.infra.database.core.DefaultDatabase;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereSchema;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereTable;
import org.apache.shardingsphere.sharding.exception.metadata.DuplicateIndexException;
import org.apache.shardingsphere.sharding.exception.metadata.IndexNotExistedException;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.index.IndexNameSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.index.IndexSegment;
import org.apache.shardingsphere.sql.parser.statement.core.value.identifier.IdentifierValue;
import org.apache.shardingsphere.sql.parser.statement.postgresql.ddl.PostgreSQLAlterIndexStatement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShardingAlterIndexSupportedCheckerTest {
    
    @Mock
    private ShardingRule shardingRule;
    
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ShardingSphereDatabase database;
    
    @Test
    void assertCheckWhenIndexExistRenameIndexNotExistForPostgreSQL() {
        PostgreSQLAlterIndexStatement sqlStatement = new PostgreSQLAlterIndexStatement();
        sqlStatement.setIndex(new IndexSegment(0, 0, new IndexNameSegment(0, 0, new IdentifierValue("t_order_index"))));
        sqlStatement.setRenameIndex(new IndexSegment(0, 0, new IndexNameSegment(0, 0, new IdentifierValue("t_order_index_new"))));
        ShardingSphereTable table = mock(ShardingSphereTable.class);
        ShardingSphereSchema schema = mock(ShardingSphereSchema.class);
        when(schema.getAllTables()).thenReturn(Collections.singleton(table));
        when(table.containsIndex("t_order_index")).thenReturn(true);
        when(table.containsIndex("t_order_index_new")).thenReturn(false);
        assertDoesNotThrow(() -> new ShardingAlterIndexSupportedChecker().check(shardingRule, database, schema, new AlterIndexStatementContext(sqlStatement, DefaultDatabase.LOGIC_NAME)));
    }
    
    @Test
    void assertCheckWhenIndexNotExistRenameIndexNotExistForPostgreSQL() {
        PostgreSQLAlterIndexStatement sqlStatement = new PostgreSQLAlterIndexStatement();
        sqlStatement.setIndex(new IndexSegment(0, 0, new IndexNameSegment(0, 0, new IdentifierValue("t_order_index"))));
        sqlStatement.setRenameIndex(new IndexSegment(0, 0, new IndexNameSegment(0, 0, new IdentifierValue("t_order_index_new"))));
        ShardingSphereTable table = mock(ShardingSphereTable.class);
        when(database.getSchema("public").getTable("t_order")).thenReturn(table);
        assertThrows(IndexNotExistedException.class, () -> new ShardingAlterIndexSupportedChecker().check(shardingRule, database, mock(ShardingSphereSchema.class),
                new AlterIndexStatementContext(sqlStatement, DefaultDatabase.LOGIC_NAME)));
    }
    
    @Test
    void assertCheckAlterIndexWhenIndexExistRenameIndexExistForPostgreSQL() {
        PostgreSQLAlterIndexStatement sqlStatement = new PostgreSQLAlterIndexStatement();
        sqlStatement.setIndex(new IndexSegment(0, 0, new IndexNameSegment(0, 0, new IdentifierValue("t_order_index"))));
        sqlStatement.setRenameIndex(new IndexSegment(0, 0, new IndexNameSegment(0, 0, new IdentifierValue("t_order_index_new"))));
        ShardingSphereSchema schema = mock(ShardingSphereSchema.class);
        ShardingSphereTable table = mock(ShardingSphereTable.class);
        when(schema.getAllTables()).thenReturn(Collections.singleton(table));
        when(table.containsIndex("t_order_index")).thenReturn(true);
        when(table.containsIndex("t_order_index_new")).thenReturn(true);
        assertThrows(DuplicateIndexException.class,
                () -> new ShardingAlterIndexSupportedChecker().check(shardingRule, database, schema, new AlterIndexStatementContext(sqlStatement, DefaultDatabase.LOGIC_NAME)));
    }
}
