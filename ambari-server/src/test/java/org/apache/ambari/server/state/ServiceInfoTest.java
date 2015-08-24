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

package org.apache.ambari.server.state;

import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * ServiceInfo tests.
 */
public class ServiceInfoTest {

  @Test
  public void testIsRestartRequiredAfterRackChange() throws Exception {

    String serviceInfoXml = "<metainfo>\n" +
        "  <schemaVersion>2.0</schemaVersion>\n" +
        "  <services>\n" +
        "    <service>\n" +
        "      <name>RESTART</name>\n" +
        "      <displayName>RESTART</displayName>\n" +
        "      <comment>Apache Hadoop Distributed File System</comment>\n" +
        "      <version>2.1.0.2.0</version>\n" +
        "      <restartRequiredAfterRackChange>true</restartRequiredAfterRackChange>\n" +
        "    </service>\n" +
        "    <service>\n" +
        "      <name>NO_RESTART</name>\n" +
        "      <displayName>NO_RESTART</displayName>\n" +
        "      <comment>Apache Hadoop Distributed File System</comment>\n" +
        "      <version>2.1.0.2.0</version>\n" +
        "      <restartRequiredAfterRackChange>false</restartRequiredAfterRackChange>\n" +
        "    </service>\n" +
        "    <service>\n" +
        "      <name>DEFAULT_RESTART</name>\n" +
        "      <displayName>DEFAULT_RESTART</displayName>\n" +
        "      <comment>Apache Hadoop Distributed File System</comment>\n" +
        "      <version>2.1.0.2.0</version>\n" +
        "    </service>\n" +
        "    <service>\n" +
        "      <name>HCFS_SERVICE</name>\n" +
        "      <displayName>HCFS_SERVICE</displayName>\n" +
        "      <comment>Hadoop Compatible File System</comment>\n" +
        "      <version>2.1.1.0</version>\n" +
        "      <serviceType>HCFS</serviceType>\n" +
        "    </service>\n" +
        "  </services>\n" +
        "</metainfo>\n";
    
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);

    assertTrue(serviceInfoMap.get("RESTART").isRestartRequiredAfterRackChange());
    assertFalse(serviceInfoMap.get("NO_RESTART").isRestartRequiredAfterRackChange());
    assertNull(serviceInfoMap.get("DEFAULT_RESTART").isRestartRequiredAfterRackChange());
    assertEquals(serviceInfoMap.get("HCFS_SERVICE").getServiceType(),"HCFS");
  }

  @Test
  public void testCustomMetricsWidgetsFiles() throws Exception {

    String serviceInfoXml = "<metainfo>\n" +
            "  <schemaVersion>2.0</schemaVersion>\n" +
            "  <services>\n" +
            "    <service>\n" +
            "      <name>CUSTOM</name>\n" +
            "      <displayName>CUSTOM</displayName>\n" +
            "      <metricsFileName>CUSTOM_metrics.json</metricsFileName>\n" +
            "      <widgetsFileName>CUSTOM_widgets.json</widgetsFileName>\n" +
            "    </service>\n" +
            "    <service>\n" +
            "      <name>DEFAULT</name>\n" +
            "      <displayName>DEFAULT</displayName>\n" +
            "      <comment>Apache Hadoop Distributed File System</comment>\n" +
            "      <version>2.1.0.2.0</version>\n" +
            "    </service>\n" +
            "  </services>\n" +
            "</metainfo>\n";

    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);

    assertEquals("CUSTOM_metrics.json", serviceInfoMap.get("CUSTOM").getMetricsFileName());
    assertEquals("CUSTOM_widgets.json", serviceInfoMap.get("CUSTOM").getWidgetsFileName());
    assertEquals("metrics.json", serviceInfoMap.get("DEFAULT").getMetricsFileName());
    assertEquals("widgets.json", serviceInfoMap.get("DEFAULT").getWidgetsFileName());
  }

  @Test
  public void testSetRestartRequiredAfterRackChange() throws Exception {
    ServiceInfo serviceInfo = new ServiceInfo();

    serviceInfo.setRestartRequiredAfterRackChange(true);
    assertTrue(serviceInfo.isRestartRequiredAfterRackChange());

    serviceInfo.setRestartRequiredAfterRackChange(false);
    assertFalse(serviceInfo.isRestartRequiredAfterRackChange());
  }

  @Test
  public void testServiceProperties() throws Exception {
    String serviceInfoXml =
      "<metainfo>" +
      "  <schemaVersion>2.0</schemaVersion>" +
      "  <services>" +
      "    <service>" +
      "      <name>WITH_PROPS</name>" +
      "      <displayName>WITH_PROPS</displayName>" +
      "      <properties>" +
      "        <property>" +
      "          <name>PROP1</name>" +
      "          <value>VAL1</value>" +
      "        </property>" +
      "        <property>" +
      "          <name>PROP2</name>" +
      "          <value>VAL2</value>" +
      "        </property>" +
      "      </properties>" +
      "    </service>" +
      "  </services>" +
      "</metainfo>";

    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);

    Map<String, String> serviceProperties = serviceInfoMap.get("WITH_PROPS").getServiceProperties();

    assertTrue(serviceProperties.containsKey("PROP1"));
    assertEquals("VAL1", serviceProperties.get("PROP1"));

    assertTrue(serviceProperties.containsKey("PROP2"));
    assertEquals("VAL2", serviceProperties.get("PROP2"));

  }


  @Test(expected = DuplicateServicePropertyException.class)
  public void testDupeServicePropertiesThrowsException() throws Exception{
    String serviceInfoXml =
      "<metainfo>" +
        "  <schemaVersion>2.0</schemaVersion>" +
        "  <services>" +
        "    <service>" +
        "      <name>WITH_PROPS</name>" +
        "      <displayName>WITH_PROPS</displayName>" +
        "      <properties>" +
        "        <property>" +
        "          <name>PROP1</name>" +
        "          <value>VAL1</value>" +
        "        </property>" +
        "        <property>" +
        "          <name>PROP1</name>" +
        "          <value>VAL2</value>" +
        "        </property>" +
        "      </properties>" +
        "    </service>" +
        "  </services>" +
        "</metainfo>";

    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);

    Map<String, String> serviceProperties = serviceInfoMap.get("WITH_PROPS").getServiceProperties();
  }

  @Test
  public void testDefaultVisibilityServiceProperties() throws Exception {
    // Given
    String serviceInfoXml =
      "<metainfo>" +
        "  <schemaVersion>2.0</schemaVersion>" +
        "  <services>" +
        "    <service>" +
        "      <name>WITH_PROPS</name>" +
        "      <displayName>WITH_PROPS</displayName>" +
        "      <properties>" +
        "        <property>" +
        "          <name>PROP1</name>" +
        "          <value>VAL1</value>" +
        "        </property>" +
        "        <property>" +
        "          <name>PROP2</name>" +
        "          <value>VAL2</value>" +
        "        </property>" +
        "      </properties>" +
        "    </service>" +
        "  </services>" +
        "</metainfo>";

    // When
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);

    // Then
    Map<String, String> serviceProperties = serviceInfoMap.get("WITH_PROPS").getServiceProperties();


    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_INSTALLABLE_PROPERTY.getKey())));
    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_MANAGED_PROPERTY.getKey())));
    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_MONITORED_PROPERTY.getKey())));
  }

  @Test
  public void testVisibilityServicePropertyOverride() throws Exception {
    // Given
    String serviceInfoXml =
      "<metainfo>" +
        "  <schemaVersion>2.0</schemaVersion>" +
        "  <services>" +
        "    <service>" +
        "      <name>WITH_PROPS</name>" +
        "      <displayName>WITH_PROPS</displayName>" +
        "      <properties>" +
        "        <property>" +
        "          <name>PROP1</name>" +
        "          <value>VAL1</value>" +
        "        </property>" +
        "        <property>" +
        "          <name>PROP2</name>" +
        "          <value>VAL2</value>" +
        "        </property>" +
        "        <property>" +
        "          <name>managed</name>" +
        "          <value>false</value>" +
        "        </property>" +
        "      </properties>" +
        "    </service>" +
        "  </services>" +
        "</metainfo>";

    // When
    Map<String, ServiceInfo> serviceInfoMap = getServiceInfo(serviceInfoXml);

    // Then
    Map<String, String> serviceProperties = serviceInfoMap.get("WITH_PROPS").getServiceProperties();


    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_INSTALLABLE_PROPERTY.getKey())));
    assertTrue("false".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_MANAGED_PROPERTY.getKey())));
    assertTrue("true".equals(serviceProperties.get(ServiceInfo.DEFAULT_SERVICE_MONITORED_PROPERTY.getKey())));

  }

  public static Map<String, ServiceInfo> getServiceInfo(String xml) throws JAXBException {
    InputStream configStream = new ByteArrayInputStream(xml.getBytes());
    JAXBContext jaxbContext = JAXBContext.newInstance(ServiceMetainfoXml.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    ServiceMetainfoXml serviceMetainfoXml = (ServiceMetainfoXml) unmarshaller.unmarshal(configStream);

    Map<String, ServiceInfo> serviceInfoMap = new HashMap<String, ServiceInfo>();
    for (ServiceInfo serviceInfo : serviceMetainfoXml.getServices()) {
      serviceInfoMap.put(serviceInfo.getName(), serviceInfo);
    }
    return serviceInfoMap;
  }
}


