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

import java.util.Map;

import javax.inject.Inject;

import org.apache.ambari.server.orm.dao.CleanupDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlertHistoryCleanupService implements CleanupService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AlertHistoryCleanupService.class);

  @Inject
  private CleanupDAO cleanupDAO;

  @Override
  public void cleanup(CleanupPolicy cleanupPolicy) {
    LOGGER.debug("Cleaning up alert history with retention policy: {}", cleanupPolicy);
    long clusterId = getClusterId((Map<String, Long>) cleanupPolicy.selectionCriteria());
    long afterDate = getAfterDate((Map<String, Long>) cleanupPolicy.selectionCriteria());

    switch (cleanupPolicy.purgePolicy()) {
      case DELETE:
        cleanupDAO.cleanAlertNoticesForClusterAfterDate(clusterId, afterDate);
        cleanupDAO.cleanAlertCurrentsForClusterAfterDate(clusterId, afterDate);
        cleanupDAO.cleanAlertHistoriesForClusterAfterDate(clusterId, afterDate);
        break;
      case ARCHIVE:
        LOGGER.warn("Unsupported cleanup type {}", cleanupPolicy.purgePolicy());
        throw new UnsupportedOperationException("Archival not yet suported during cleanup!");
      default:
        LOGGER.error("Cleanup Type must be set!");
        throw new IllegalArgumentException("Cleanup Type must be set!");
    }
  }

  private long getAfterDate(Map<String, Long> criteriaMap) {
    return criteriaMap.get("afterDate");
  }

  private long getClusterId(Map<String, Long> criteriaMap) {
    return criteriaMap.get("clusterId");
  }


}