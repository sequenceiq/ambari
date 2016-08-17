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
package org.apache.ambari.server.checks;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.easymock.EasyMock.expect;


public class DatabaseConsistencyCheckHelperTest {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private DBAccessor mockDBDbAccessor;

  @Mock(type = MockType.NICE)
  private Connection mockConnection;

  @Mock(type = MockType.NICE)
  private ResultSet mockResultSet;

  @Mock(type = MockType.NICE)
  private Statement mockStatement;

  @Mock(type = MockType.NICE)
  private StackManagerFactory mockStackManagerFactory;

  @Mock(type = MockType.NICE)
  private EntityManager mockEntityManager;

  @Mock(type = MockType.NICE)
  private Clusters mockClusters;

  @Mock(type = MockType.NICE)
  private OsFamily mockOSFamily;

  @Mock(type = MockType.NICE)
  private AmbariMetaInfo mockAmbariMetainfo;

  @Mock(type = MockType.NICE)
  private ResultSet stackResultSet;

  @Mock(type = MockType.NICE)
  private ResultSet serviceConfigResultSet;

  @Mock(type = MockType.NICE)
  private ServiceInfo mockHDFSServiceInfo;

  @TestSubject
  private DatabaseConsistencyCheckHelper databaseConsistencyCheckHelper = new DatabaseConsistencyCheckHelper();

  @Before
  public void before() {
    EasyMockSupport.injectMocks(databaseConsistencyCheckHelper);
  }

  @After
  public void after(){
    EasyMock.reset(mockDBDbAccessor, mockConnection, mockResultSet, mockStatement, mockStackManagerFactory, mockEntityManager, mockClusters, mockOSFamily);
  }


  @Test
  public void testCheckForNotMappedConfigs() throws Exception {
    expect(mockDBDbAccessor.getConnection()).andReturn(mockConnection);
    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select type_name from clusterconfig where type_name not in (select type_name from clusterconfigmapping)")).andReturn(mockResultSet);

    EasyMock.replay(mockDBDbAccessor, mockConnection, mockResultSet, mockStatement, mockStackManagerFactory, mockEntityManager, mockClusters, mockOSFamily);

    databaseConsistencyCheckHelper.checkForNotMappedConfigsToCluster();

    EasyMock.verify(mockDBDbAccessor, mockConnection, mockStatement);
  }

  @Test
  public void testCheckForConfigsSelectedMoreThanOnce() throws Exception {

    expect(mockDBDbAccessor.getConnection()).andReturn(mockConnection);
    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select c.cluster_name, ccm.type_name from clusterconfigmapping ccm " +
        "join clusters c on ccm.cluster_id=c.cluster_id " +
        "group by c.cluster_name, ccm.type_name " +
        "having sum(selected) > 1")).andReturn(mockResultSet);

    EasyMock.replay(mockDBDbAccessor, mockConnection, mockResultSet, mockStatement, mockStackManagerFactory, mockEntityManager, mockClusters, mockOSFamily);

    databaseConsistencyCheckHelper.checkForConfigsSelectedMoreThanOnce();

    EasyMock.verify(mockDBDbAccessor, mockConnection, mockStatement);
  }

  @Test
  public void testCheckForHostsWithoutState() throws Exception {
    expect(mockDBDbAccessor.getConnection()).andReturn(mockConnection);
    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select host_name from hosts where host_id not in (select host_id from hoststate)")).andReturn(mockResultSet);

    EasyMock.replay(mockDBDbAccessor, mockConnection, mockResultSet, mockStatement, mockStackManagerFactory, mockEntityManager, mockClusters, mockOSFamily);

    databaseConsistencyCheckHelper.checkForHostsWithoutState();

    EasyMock.verify(mockDBDbAccessor, mockConnection, mockStatement);
  }

  @Test
  public void testCheckHostComponentStatesCountEqualsHostComponentsDesiredStates() throws Exception {

    expect(mockDBDbAccessor.getConnection()).andReturn(mockConnection);
    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select count(*) from hostcomponentstate")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select count(*) from hostcomponentdesiredstate")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select count(*) FROM hostcomponentstate hcs " +
        "JOIN hostcomponentdesiredstate hcds ON hcs.service_name=hcds.service_name AND " +
        "hcs.component_name=hcds.component_name AND hcs.host_id=hcds.host_id")).andReturn(mockResultSet);

    EasyMock.replay(mockDBDbAccessor, mockConnection, mockResultSet, mockStatement, mockStackManagerFactory, mockEntityManager, mockClusters, mockOSFamily);

    databaseConsistencyCheckHelper.checkHostComponentStatesCountEqualsHostComponentsDesiredStates();

    EasyMock.verify(mockDBDbAccessor, mockConnection, mockStatement);
  }

  @Test
  public void testCheckServiceConfigs() throws Exception {

    Map<String, ServiceInfo> services = new HashMap<>();
    services.put("HDFS", mockHDFSServiceInfo);

    Map<String, Map<String, Map<String, String>>> configAttributes = new HashMap<>();
    configAttributes.put("core-site", new HashMap<String, Map<String, String>>());

    expect(mockDBDbAccessor.getConnection()).andReturn(mockConnection);
    expect(mockHDFSServiceInfo.getConfigTypeAttributes()).andReturn(configAttributes);
    expect(mockAmbariMetainfo.getServices("HDP", "2.2")).andReturn(services);
    expect(serviceConfigResultSet.next()).andReturn(true);
    expect(serviceConfigResultSet.getString("service_name")).andReturn("HDFS");
    expect(serviceConfigResultSet.getString("type_name")).andReturn("core-site");
    expect(stackResultSet.next()).andReturn(true);
    expect(stackResultSet.getString("stack_name")).andReturn("HDP");
    expect(stackResultSet.getString("stack_version")).andReturn("2.2");
    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select c.cluster_name, service_name from clusterservices cs " +
        "join clusters c on cs.cluster_id=c.cluster_id " +
        "where service_name not in (select service_name from serviceconfig sc where sc.cluster_id=cs.cluster_id and sc.service_name=cs.service_name and sc.group_id is null)")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, sc.service_name, sc.version from serviceconfig sc " +
        "join clusters c on sc.cluster_id=c.cluster_id " +
        "where service_config_id not in (select service_config_id from serviceconfigmapping) and group_id is null")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, s.stack_name, s.stack_version from clusters c " +
        "join stack s on c.desired_stack_id = s.stack_id")).andReturn(stackResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, cs.service_name, cc.type_name, sc.version from clusterservices cs " +
        "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
        "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
        "join clusterconfig cc on scm.config_id=cc.config_id and sc.cluster_id=cc.cluster_id " +
        "join clusters c on cc.cluster_id=c.cluster_id and sc.stack_id=c.desired_stack_id " +
        "where sc.group_id is null and sc.service_config_id=(select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
        "group by c.cluster_name, cs.service_name, cc.type_name, sc.version")).andReturn(serviceConfigResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, cs.service_name, cc.type_name from clusterservices cs " +
        "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
        "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
        "join clusterconfig cc on scm.config_id=cc.config_id and cc.cluster_id=sc.cluster_id " +
        "join clusterconfigmapping ccm on cc.type_name=ccm.type_name and cc.version_tag=ccm.version_tag and cc.cluster_id=ccm.cluster_id " +
        "join clusters c on ccm.cluster_id=c.cluster_id " +
        "where sc.group_id is null and sc.service_config_id = (select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
        "group by c.cluster_name, cs.service_name, cc.type_name " +
        "having sum(ccm.selected) < 1")).andReturn(mockResultSet);


    EasyMock.replay(mockDBDbAccessor, mockConnection, mockResultSet, mockStatement, mockStackManagerFactory, mockEntityManager, mockClusters, mockOSFamily);

    mockAmbariMetainfo.init();

    databaseConsistencyCheckHelper.checkServiceConfigs();

    EasyMock.verify(mockDBDbAccessor, mockConnection, mockStatement);
  }

}

