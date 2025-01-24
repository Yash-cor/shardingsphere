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

package org.apache.shardingsphere.mode.fixture;

import org.apache.shardingsphere.mode.metadata.MetaDataContextManager;
import org.apache.shardingsphere.mode.persist.service.MetaDataManagerPersistService;
import org.apache.shardingsphere.mode.persist.service.PersistServiceBuilder;
import org.apache.shardingsphere.mode.persist.service.ProcessPersistService;
import org.apache.shardingsphere.mode.spi.repository.PersistRepository;

public final class PersistServiceBuilderFixture implements PersistServiceBuilder {
    
    @Override
    public MetaDataManagerPersistService buildMetaDataManagerPersistService(final PersistRepository repository, final MetaDataContextManager metaDataContextManager) {
        return null;
    }
    
    @Override
    public ProcessPersistService buildProcessPersistService(final PersistRepository repository) {
        return null;
    }
    
    @Override
    public Object getType() {
        return "foo_type";
    }
    
    @Override
    public boolean isDefault() {
        return true;
    }
}
