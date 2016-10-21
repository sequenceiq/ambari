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

package org.apache.ambari.server.hooks.users;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.orm.AmbariJpaLocalTxnInterceptor;
import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class UserHookServiceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserHookServiceTest.class);

  private Injector injector;

  @Before
  public void before() throws Exception {
    injector = Guice.createInjector(new ControllerModule());
  }

  @Test
  public void testGuiceContext() {
    AmbariJpaLocalTxnInterceptor interceptor = injector.getInstance(AmbariJpaLocalTxnInterceptor.class);
    Assert.assertNotNull(interceptor);
  }

  @TestSubject
  private HookService hookService = new UserHookService();

  @Test
  public void shouldMapBeSerializedToString() throws Exception {
    Map<String, List<String>> usersToGroups = new HashMap<>();

    List<String> testGroups = Arrays.asList("yarn", "hdfs");
    usersToGroups.put("user", testGroups);
    usersToGroups.put("user1", testGroups);

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(usersToGroups);
    LOGGER.info("Map as json: {}", json);

    usersToGroups = mapper.readValue(json, Map.class);
    LOGGER.info("Map as json: {}", usersToGroups);

  }
}