/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.cleanup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator for CleanupServices.
 * It allows chaining of CleanupService implemantations, thus different cleanup logic implementations can be kept
 * separated.
 */
public class ChainedCleanupService implements CleanupService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChainedCleanupService.class);

  private Set<CleanupService> cleanupServiceRegistry;

  @Inject
  protected ChainedCleanupService(Set<CleanupService> cleanupServiceRegistry) {
    this.cleanupServiceRegistry = cleanupServiceRegistry;
  }

  @Override
  public void cleanup(CleanupPolicy cleanupPolicy) {
    LOGGER.debug("Runing cleanup with retention policy: {}", cleanupPolicy);
    for (CleanupService cleanupService : cleanupServiceRegistry) {
      LOGGER.debug("Executing cleanup service: {} with retention policy: {}", cleanupService, cleanupPolicy);
      cleanupService.cleanup(cleanupPolicy);
    }
    LOGGER.debug("Cleanup DONE");
  }

  protected Collection<String> showCleanupRegistry() {
    List<String> services = new ArrayList<String>();
    for (CleanupService service : cleanupServiceRegistry) {
      LOGGER.debug("Registered CleanupService: {}", service.getClass().getName());
      services.add(service.getClass().getName());
    }
    return services;
  }
}
