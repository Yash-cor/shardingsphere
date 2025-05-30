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

package org.apache.shardingsphere.sqlfederation.compiler.metadata.catalog;

import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.validate.SqlNameMatchers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * SQL federation catalog reader.
 */
public final class SQLFederationCatalogReader extends CalciteCatalogReader {
    
    public SQLFederationCatalogReader(final CalciteSchema rootSchema, final List<String> schemaPaths, final RelDataTypeFactory typeFactory, final CalciteConnectionConfig config) {
        super(rootSchema, SqlNameMatchers.withCaseSensitive(config.caseSensitive()), Arrays.asList(Collections.emptyList(), schemaPaths), typeFactory, config);
    }
}
