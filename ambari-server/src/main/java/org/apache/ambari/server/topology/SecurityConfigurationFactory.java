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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import com.google.common.base.Enums;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.state.SecurityType;

import java.util.Map;

@StaticallyInject
public class SecurityConfigurationFactory {

  public static final String SECURITY_PROPERTY_ID = "security";
  public static final String TYPE_PROPERTY_ID = "type";
  public static final String KERBEROS_DESCRIPTOR_PROPERTY_ID = "kerberos_descriptor";
  public static final String KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID = "kerberos_descriptor_reference";

  @Inject
  protected static Gson jsonSerializer;

  /**
   * Creates and also validates SecurityConfiguration based on properties parsed from request Json.
   * @param properties Security properties from Json parsed into a Map
   * @return
   */
  public SecurityConfiguration createSecurityConfigurationFromRequest(Map<String, Object> properties) {
    Map<String, Object> securityProperties = (Map<String, Object>) properties.get(SECURITY_PROPERTY_ID);
    if (securityProperties == null) {
      return null;
    }

    String securityTypeString = Strings.emptyToNull((String) securityProperties.get(TYPE_PROPERTY_ID));
    if (securityTypeString == null) {
      throw new IllegalArgumentException("Type missing from security block.");
    }

    SecurityType securityType = Enums.getIfPresent(SecurityType.class, securityTypeString).orNull();
    if (securityType == null) {
      throw new IllegalArgumentException("Invalid security type specified: " + securityTypeString);
    }

    if (securityType == SecurityType.KERBEROS) {
      String descriptorReference = Strings.emptyToNull((String) securityProperties.get(KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID));
      Object descriptorJsonMap = securityProperties.get(KERBEROS_DESCRIPTOR_PROPERTY_ID);

      if (descriptorReference == null && descriptorJsonMap == null) {
        throw new IllegalArgumentException(KERBEROS_DESCRIPTOR_PROPERTY_ID + " or " + KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID + " is required for KERBEROS security setup.");
      } else if (descriptorReference != null && descriptorJsonMap != null) {
        throw new IllegalArgumentException("Usage of properties : " + KERBEROS_DESCRIPTOR_PROPERTY_ID + " and " + KERBEROS_DESCRIPTOR_REFERENCE_PROPERTY_ID + " at the same time, is not allowed.");
      } else if (descriptorJsonMap != null) {
        String descriptor = jsonSerializer.<Map<String, Object>>toJson(descriptorJsonMap, Map.class);
        return new SecurityConfiguration(securityType, null, descriptor);
      } else {
        return new SecurityConfiguration(securityType, descriptorReference, null);
      }

    } else {
      return new SecurityConfiguration(SecurityType.NONE);
    }

  }

}
