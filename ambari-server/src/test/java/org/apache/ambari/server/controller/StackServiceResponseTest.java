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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.state.ServiceInfo;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;



public class StackServiceResponseTest {

  private ServiceInfo serviceInfo;

  @Before
  public void setUp() {
    serviceInfo = new ServiceInfo();
  }

  @Test
  public void testIsInstallableTrue() throws Exception {
    serviceInfo.setInstallable(true);
    StackServiceResponse stackServiceResponse = new StackServiceResponse(serviceInfo);

    assertTrue(stackServiceResponse.isInstallable());
  }

  @Test
  public void testIsInstallableFalse() throws Exception {
    serviceInfo.setInstallable(false);
    StackServiceResponse stackServiceResponse = new StackServiceResponse(serviceInfo);

    assertFalse(stackServiceResponse.isInstallable());
  }

  @Test
  public void testIsManagedTrue() throws Exception {
    serviceInfo.setManaged(true);
    StackServiceResponse stackServiceResponse = new StackServiceResponse(serviceInfo);

    assertTrue(stackServiceResponse.isManaged());
  }

  @Test
  public void testIsManagedFalse() throws Exception {
    serviceInfo.setManaged(false);
    StackServiceResponse stackServiceResponse = new StackServiceResponse(serviceInfo);

    assertFalse(stackServiceResponse.isManaged());
  }

  @Test
  public void testIsMonitoredTrue() throws Exception {
    serviceInfo.setMonitored(true);
    StackServiceResponse stackServiceResponse = new StackServiceResponse(serviceInfo);

    assertTrue(stackServiceResponse.isMonitored());
  }

  @Test
  public void testIsMonitoredFalse() throws Exception {
    serviceInfo.setMonitored(false);
    StackServiceResponse stackServiceResponse = new StackServiceResponse(serviceInfo);

    assertFalse(stackServiceResponse.isMonitored());
  }

}
