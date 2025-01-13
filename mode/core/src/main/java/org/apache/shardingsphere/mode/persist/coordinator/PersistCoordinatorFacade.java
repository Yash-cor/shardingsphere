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

package org.apache.shardingsphere.mode.persist.coordinator;

import lombok.Getter;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.mode.spi.repository.PersistRepository;

/**
 * Persist coordinator facade.
 */
@Getter
public final class PersistCoordinatorFacade {
    
    private final ProcessPersistCoordinator processPersistCoordinator;
    
    public PersistCoordinatorFacade(final PersistRepository repository, final ModeConfiguration modeConfig) {
        PersistCoordinatorFacadeBuilder builder = TypedSPILoader.getService(PersistCoordinatorFacadeBuilder.class, modeConfig.getType());
        processPersistCoordinator = builder.buildProcessPersistCoordinator(repository);
    }
}