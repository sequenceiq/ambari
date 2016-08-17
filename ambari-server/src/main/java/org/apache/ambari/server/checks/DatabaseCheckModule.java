/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.checks;

import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessorImpl;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.SecurityHelperImpl;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceComponentImpl;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceImpl;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.configgroup.ConfigGroupImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.host.HostImpl;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionImpl;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;

public class DatabaseCheckModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DBAccessor.class).to(DBAccessorImpl.class);
    bind(Clusters.class).to(ClustersImpl.class);
    bind(SecurityHelper.class).toInstance(SecurityHelperImpl.getInstance());

    final SessionIdManager sessionIdManager = new HashSessionIdManager();
    final SessionManager sessionManager = new HashSessionManager();
    sessionManager.getSessionCookieConfig().setPath("/");
    sessionManager.setSessionIdManager(sessionIdManager);
    bind(SessionManager.class).toInstance(sessionManager);
    bind(SessionIdManager.class).toInstance(sessionIdManager);


    // Host role commands status summary max cache enable/disable
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_ENABLED)).to(Boolean.FALSE);

    // Host role commands status summary max cache size
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_SIZE)).to(10L);

    // Host role command status summary cache expiry duration in minutes
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES)).to(10L);

    install(new FactoryModuleBuilder().build(StackManagerFactory.class));
    install(new FactoryModuleBuilder().implement(Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
    install(new FactoryModuleBuilder().implement(Host.class, HostImpl.class).build(HostFactory.class));
    install(new FactoryModuleBuilder().implement(Config.class, ConfigImpl.class).build(ConfigFactory.class));
    install(new FactoryModuleBuilder().implement(ConfigGroup.class, ConfigGroupImpl.class).build(ConfigGroupFactory.class));
    install(new FactoryModuleBuilder().implement(RequestExecution.class, RequestExecutionImpl.class).build(RequestExecutionFactory.class));
    install(new FactoryModuleBuilder().implement(Service.class, ServiceImpl.class).build(ServiceFactory.class));
    install(new FactoryModuleBuilder().implement(ServiceComponent.class, ServiceComponentImpl.class).build(ServiceComponentFactory.class));
    install(new FactoryModuleBuilder().implement(ServiceComponentHost.class, ServiceComponentHostImpl.class).build(ServiceComponentHostFactory.class));

  }
}
