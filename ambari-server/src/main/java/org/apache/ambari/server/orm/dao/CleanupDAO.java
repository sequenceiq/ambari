/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.orm.dao;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class CleanupDAO {
  private static final Logger LOGGER = LoggerFactory.getLogger(CleanupDAO.class);

  @Inject
  private Provider<EntityManager> emProvider;


  @Transactional
  public void cleanAlertNoticesForClusterAfterDate(Long clusterId, long afterDate) {
    executeQuery("AlertNoticeEntity.removeByAlertHistoryAfterDate", AlertNoticeEntity.class, clusterId, afterDate);
  }

  @Transactional
  public void cleanAlertHistoriesForClusterAfterDate(Long clusterId, long afterDate) {
    executeQuery("AlertHistoryEntity.removeByClusterAfterDate", AlertHistoryEntity.class, clusterId, afterDate);
  }

  @Transactional
  public void cleanAlertCurrentsForClusterAfterDate(long clusterId, long afterDate) {
    executeQuery("AlertCurrentEntity.removeByAlertHistoryAfterDate", AlertCurrentEntity.class, clusterId, afterDate);
  }


  private int executeQuery(String namedQuery, Class entityType, long clusterId, long afterDate) {
    LOGGER.info("Cleaning up [ {} ] entities for cluster id [ {} ] after date [ {} ]", entityType, clusterId, afterDate);
    TypedQuery<AlertHistoryEntity> query = emProvider.get().createNamedQuery(namedQuery, entityType);
    query.setParameter("clusterId", clusterId);
    query.setParameter("afterDate", afterDate);
    int affectedRows = query.executeUpdate();
    LOGGER.debug("Affected rows: [ {} ].", affectedRows);
    return affectedRows;
  }

}
