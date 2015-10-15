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

package org.apache.ambari.server.topology;

import com.google.gson.Gson;
import org.apache.ambari.server.state.SecurityType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * SecurityConfigurationFactory unit tests.
 */
@SuppressWarnings("unchecked")
public class SecurityConfigurationFactoryTest {

  SecurityConfigurationFactory factory = new SecurityConfigurationFactory();

  @BeforeClass
  public static void setup() {
    SecurityConfigurationFactory.jsonSerializer = new Gson();
  }

  @Test
  public void testCreateKerberosSecurityWithReferenceDescriptor() throws Exception {
    Map<String, Object> reuqestMap = new HashMap<>();
    Map<String, Object> security = new HashMap<>();
    security.put(SecurityConfigurationFactory.TYPE_PROPERTY_ID, SecurityType.KERBEROS.toString());
    security.put(SecurityConfigurationFactory.KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID, "testRef");
    reuqestMap.put(SecurityConfigurationFactory.SECURITY_PROPERTY_ID, security);

    SecurityConfiguration securityConfiguration = factory.createSecurityConfigurationFromRequest(reuqestMap);

    assertTrue(securityConfiguration.getType() == SecurityType.KERBEROS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateKerberosSecurityWithoutDescriptor() throws Exception {
    Map<String, Object> reuqestMap = new HashMap<>();
    Map<String, Object> security = new HashMap<>();
    security.put(SecurityConfigurationFactory.TYPE_PROPERTY_ID, SecurityType.KERBEROS.toString());
    reuqestMap.put(SecurityConfigurationFactory.SECURITY_PROPERTY_ID, security);

    SecurityConfiguration securityConfiguration = factory.createSecurityConfigurationFromRequest(reuqestMap);

    assertTrue(securityConfiguration.getType() == SecurityType.KERBEROS);
  }

  @Test
  public void testCreateEmpty() throws Exception {
    Map<String, Object> reuqestMap = new HashMap<>();

    SecurityConfiguration securityConfiguration = factory.createSecurityConfigurationFromRequest(reuqestMap);

    assertTrue(securityConfiguration == null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateInvalidSecurityType() throws Exception {
    Map<String, Object> reuqestMap = new HashMap<>();
    Map<String, Object> security = new HashMap<>();
    security.put(SecurityConfigurationFactory.TYPE_PROPERTY_ID, "INVALID_SECURITY_TYPE");
    reuqestMap.put(SecurityConfigurationFactory.SECURITY_PROPERTY_ID, security);

    SecurityConfiguration securityConfiguration = factory.createSecurityConfigurationFromRequest(reuqestMap);

    assertTrue(securityConfiguration.getType() == SecurityType.KERBEROS);
  }

  @Test
  public void testCreateKerberosSecurityTypeNone() throws Exception {
    Map<String, Object> reuqestMap = new HashMap<>();
    Map<String, Object> security = new HashMap<>();
    security.put(SecurityConfigurationFactory.TYPE_PROPERTY_ID, SecurityType.NONE.toString());
    reuqestMap.put(SecurityConfigurationFactory.SECURITY_PROPERTY_ID, security);

    SecurityConfiguration securityConfiguration = factory.createSecurityConfigurationFromRequest(reuqestMap);

    assertTrue(securityConfiguration.getType() == SecurityType.NONE);
  }

}
