/**
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
package org.apache.ambari.server.events.listeners.alerts;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Services;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.InitialAlertEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link AlertReceivedListener} class handles {@link AlertReceivedEvent}
 * and updates the appropriate DAOs. It may also fire new
 * {@link AlertStateChangeEvent} when an {@link AlertState} change is detected.
 */
@Singleton
@EagerSingleton
public class AlertReceivedListener {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertReceivedListener.class);

  @Inject
  private AlertsDAO m_alertsDao;

  @Inject
  private AlertDefinitionDAO m_definitionDao;

  /**
   * Used for looking up whether an alert has a valid service/component/host
   */
  @Inject
  private Provider<Clusters> m_clusters;

  /**
   * Receives and publishes {@link AlertEvent} instances.
   */
  private AlertEventPublisher m_alertEventPublisher;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertReceivedListener(AlertEventPublisher publisher) {
    m_alertEventPublisher = publisher;
    m_alertEventPublisher.register(this);
  }

  /**
   * Adds an alert. Checks for a new state before creating a new history record.
   *
   * @param event
   *          the event to handle.
   */
  @Subscribe
  @AllowConcurrentEvents
  @RequiresSession
  public void onAlertEvent(AlertReceivedEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }

    Alert alert = event.getAlert();

    // jobs that were running when a service/component/host was changed
    // which invalidate the alert should not be reported
    if (!isValid(alert)) {
      return;
    }

    long clusterId = event.getClusterId();

    AlertDefinitionEntity definition = m_definitionDao.findByName(clusterId,
        alert.getName());

    if (null == definition) {
      LOG.warn(
          "Received an alert for {} which is a definition that does not exist anymore",
          alert.getName());

      return;
    }

    // it's possible that a definition which is disabled will still have a
    // running alert returned; this will ensure we don't record it
    if (!definition.getEnabled()) {
      LOG.debug(
          "Received an alert for {} which is disabled. No more alerts should be received for this definition.",
          alert.getName());

      return;
    }

    AlertCurrentEntity current;

    if (StringUtils.isBlank(alert.getHostName()) || definition.isHostIgnored()) {
      current = m_alertsDao.findCurrentByNameNoHost(clusterId, alert.getName());
    } else {
      current = m_alertsDao.findCurrentByHostAndName(clusterId, alert.getHostName(),
          alert.getName());
    }

    if (null == current) {
      AlertHistoryEntity history = createHistory(clusterId, definition, alert);

      current = new AlertCurrentEntity();
      current.setMaintenanceState(MaintenanceState.OFF);
      current.setAlertHistory(history);
      current.setLatestTimestamp(alert.getTimestamp());
      current.setOriginalTimestamp(alert.getTimestamp());
      m_alertsDao.create(current);

      // broadcast the initial alert being received
      InitialAlertEvent initialAlertEvent = new InitialAlertEvent(
          event.getClusterId(), event.getAlert(), current);

      m_alertEventPublisher.publish(initialAlertEvent);
    } else if (alert.getState() == current.getAlertHistory().getAlertState()) {
      current.setLatestTimestamp(alert.getTimestamp());
      current.setLatestText(alert.getText());
      m_alertsDao.merge(current);
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "Alert State Changed: CurrentId {}, CurrentTimestamp {}, HistoryId {}, HistoryState {}",
            current.getAlertId(), current.getLatestTimestamp(),
            current.getAlertHistory().getAlertId(),
            current.getAlertHistory().getAlertState());
      }

      AlertHistoryEntity oldHistory = current.getAlertHistory();
      AlertState oldState = oldHistory.getAlertState();

      // insert history, update current
      AlertHistoryEntity history = createHistory(clusterId,
          oldHistory.getAlertDefinition(), alert);

      current.setLatestTimestamp(alert.getTimestamp());
      current.setOriginalTimestamp(alert.getTimestamp());
      current.setLatestText(alert.getText());

      current = m_alertsDao.mergeAlertCurrentWithAlertHistory(current, history);

      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "Alert State Merged: CurrentId {}, CurrentTimestamp {}, HistoryId {}, HistoryState {}",
            current.getAlertId(), current.getLatestTimestamp(),
            current.getAlertHistory().getAlertId(),
            current.getAlertHistory().getAlertState());
      }

      // broadcast the alert changed event for other subscribers
      AlertStateChangeEvent alertChangedEvent = new AlertStateChangeEvent(
          event.getClusterId(), event.getAlert(), current,
          oldState);

      m_alertEventPublisher.publish(alertChangedEvent);
    }
  }

  /**
   * Gets whether the specified alert is valid for its reported cluster,
   * service, component, and host. This method is necessary for the case where a
   * component has been removed from a host, but the alert data is going to be
   * returned before the agent alert job can be unscheduled.
   *
   * @param alert
   *          the alert.
   * @return {@code true} if the alert is for a valid combination of
   *         cluster/service/component/host.
   */
  private boolean isValid(Alert alert) {
    String clusterName = alert.getCluster();
    String serviceName = alert.getService();
    String componentName = alert.getComponent();
    String hostName = alert.getHostName();

    // if the alert is not bound to a cluster, then it's most likely a
    // host alert and is always valid
    if( null == clusterName ){
      return true;
    }

    // AMBARI is always a valid service
    String ambariServiceName = Services.AMBARI.name();
    if (ambariServiceName.equals(serviceName)) {
      return true;
    }

    final Cluster cluster;
    try {
      cluster = m_clusters.get().getCluster(clusterName);
      if (null == cluster) {
        LOG.error("Unable to process alert {} for an invalid cluster named {}",
            alert.getName(), clusterName);

        return false;
      }
    } catch (AmbariException ambariException) {
      LOG.error("Unable to process alert {} for an invalid cluster named {}",
          alert.getName(), clusterName, ambariException);

      return false;
    }

    if (StringUtils.isNotBlank(hostName)) {
      // if valid hostname
      if (!m_clusters.get().hostExists(hostName)) {
        LOG.error("Unable to process alert {} for an invalid host named {}",
            alert.getName(), hostName);
        return false;
      }
      if (!cluster.getServices().containsKey(serviceName)) {
        LOG.error("Unable to process alert {} for an invalid service named {}",
            alert.getName(), serviceName);

        return false;
      }
      // if the alert is for a host/component then verify that the component
      // is actually installed on that host
      if (null != componentName &&
          !cluster.getHosts(serviceName, componentName).contains(hostName)) {
        LOG.error(
            "Unable to process alert {} for an invalid service {} and component {} on host {}",
            alert.getName(), serviceName, componentName, hostName);
        return false;
      }
    }

    return true;
  }

  /**
   * Convenience to create a new alert.
   *
   * @param clusterId
   *          the cluster id
   * @param definition
   *          the definition
   * @param alert
   *          the alert data
   * @return the new history record
   */
  private AlertHistoryEntity createHistory(long clusterId,
      AlertDefinitionEntity definition, Alert alert) {
    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setAlertDefinition(definition);
    history.setAlertLabel(definition.getLabel());
    history.setAlertInstance(alert.getInstance());
    history.setAlertState(alert.getState());
    history.setAlertText(alert.getText());
    history.setAlertTimestamp(Long.valueOf(alert.getTimestamp()));
    history.setClusterId(Long.valueOf(clusterId));
    history.setComponentName(alert.getComponent());
    history.setServiceName(alert.getService());

    // only set a host for the history item if the alert definition says to
    if (definition.isHostIgnored()) {
      history.setHostName(null);
    } else {
      history.setHostName(alert.getHostName());
    }

    return history;
  }
}
