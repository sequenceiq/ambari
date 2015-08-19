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

package org.apache.ambari.server.controller.internal;


import com.google.common.collect.ImmutableSet;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackServiceRequest;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import java.util.Set;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class StackServiceResourceProviderTest {

  private static final String  SERVICE_INSTALLABLE_ID = PropertyHelper.getPropertyId("StackServices", "installable");
  private static final Boolean TEST_IS_INSTALLABLE_TRUE = true;
  private static final Boolean TEST_IS_INSTALLABLE_FALSE = false;

  private static final String  SERVICE_MANAGED_ID = PropertyHelper.getPropertyId("StackServices", "managed");
  private static final Boolean TEST_IS_MANAGED_TRUE = true;
  private static final Boolean TEST_IS_MANAGED_FALSE = false;

  private static final String  SERVICE_MONITORED_ID = PropertyHelper.getPropertyId("StackServices", "monitored");
  private static final Boolean TEST_IS_MONITORED_TRUE = true;
  private static final Boolean TEST_IS_MONITORED_FALSE = false;


  @Test
  public void testCreateStackServiceResourceVisibilityFlagsTrue() throws Exception {
    // Given
    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    Resource.Type type = Resource.Type.StackService;


    StackServiceResponse stackServiceResponse = createNiceMock(StackServiceResponse.class);
    expect(stackServiceResponse.isInstallable()).andReturn(TEST_IS_INSTALLABLE_TRUE).anyTimes();
    expect(stackServiceResponse.isManaged()).andReturn(TEST_IS_MANAGED_TRUE).anyTimes();
    expect(stackServiceResponse.isMonitored()).andReturn(TEST_IS_MONITORED_TRUE).anyTimes();

    expect(managementController.getStackServices((Set<StackServiceRequest>) anyObject()))
      .andReturn(ImmutableSet.of(stackServiceResponse)).anyTimes();


    replay(managementController, stackServiceResponse);

    Request request = PropertyHelper.getReadRequest(SERVICE_INSTALLABLE_ID, SERVICE_MANAGED_ID, SERVICE_MONITORED_ID);

    ResourceProvider stackServiceResourceProvider = AbstractControllerResourceProvider.getResourceProvider(type,
      PropertyHelper.getPropertyIds(type),
      PropertyHelper.getKeyPropertyIds(type),
      managementController);



    // When
    Set<Resource> resources = stackServiceResourceProvider.getResources(request, null);

    // Then
    Resource expected =  new ResourceImpl(type);
    expected.setProperty(SERVICE_INSTALLABLE_ID, TEST_IS_INSTALLABLE_TRUE);
    expected.setProperty(SERVICE_MANAGED_ID, TEST_IS_MANAGED_TRUE);
    expected.setProperty(SERVICE_MONITORED_ID, TEST_IS_MONITORED_TRUE);

    assertEquals(resources, ImmutableSet.of(expected));
  }


  @Test
  public void testCreateStackServiceResourceVisibilityFlagsFalse() throws Exception {
    // Given
    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    Resource.Type type = Resource.Type.StackService;


    StackServiceResponse stackServiceResponse = createNiceMock(StackServiceResponse.class);
    expect(stackServiceResponse.isInstallable()).andReturn(TEST_IS_INSTALLABLE_FALSE).anyTimes();
    expect(stackServiceResponse.isManaged()).andReturn(TEST_IS_MANAGED_FALSE).anyTimes();
    expect(stackServiceResponse.isMonitored()).andReturn(TEST_IS_MONITORED_FALSE).anyTimes();

    expect(managementController.getStackServices((Set<StackServiceRequest>) anyObject()))
      .andReturn(ImmutableSet.of(stackServiceResponse)).anyTimes();


    replay(managementController, stackServiceResponse);

    Request request = PropertyHelper.getReadRequest(SERVICE_INSTALLABLE_ID, SERVICE_MANAGED_ID, SERVICE_MONITORED_ID);

    ResourceProvider stackServiceResourceProvider = AbstractControllerResourceProvider.getResourceProvider(type,
      PropertyHelper.getPropertyIds(type),
      PropertyHelper.getKeyPropertyIds(type),
      managementController);



    // When
    Set<Resource> resources = stackServiceResourceProvider.getResources(request, null);

    // Then
    Resource expected =  new ResourceImpl(type);
    expected.setProperty(SERVICE_INSTALLABLE_ID, TEST_IS_INSTALLABLE_FALSE);
    expected.setProperty(SERVICE_MANAGED_ID, TEST_IS_MANAGED_FALSE);
    expected.setProperty(SERVICE_MONITORED_ID, TEST_IS_MONITORED_FALSE);

    assertEquals(ImmutableSet.of(expected), resources);
  }
}
