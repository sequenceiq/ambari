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

package com.google.inject.persist.jpa;

import java.util.Properties;

import org.apache.ambari.server.checks.AmbariCoreModule;
import org.apache.ambari.server.configuration.Configuration;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.persist.PersistService;

import junit.framework.Assert;

public class AmbariJpaPersistModuleFunctionalTest {

  private static Injector injector;

  /**
   * Builds the IoC context for the module being tested.
   * (this only needs to be done once for all the tests in this class)
   */
  @BeforeClass
  public static void beforeClass() {
    injector = Guice.createInjector(new AmbariCoreModule(), new AmbariJpaPersistModule("ambari-server"));
  }

  @Test
  public void shouldPersistServiceBeBoundToGuiceContextAsSingleton() throws Exception {
    //GIVEN
    // the container is set up

    //WHEN
    PersistService persistService = injector.getInstance(AmbariJpaPersistService.class);

    // THEN
    Assert.assertNotNull("Retrieved instance should not be null", persistService);
    Assert.assertTrue("The instance should be singleton", Scopes.isSingleton(injector.getBinding(PersistService.class)));

  }

  @Test
  public void shouldJpaPropertiesBeBoundToGuiceContainerAsSingleton() throws Exception {

    Properties jpaProps = injector.getInstance(Key.get(Properties.class, Jpa.class));

    // THEN
    Assert.assertNotNull("Retrieved instance should not be null", jpaProps);
    Assert.assertTrue("The instance should be singleton", Scopes.isSingleton(injector.getBinding(Key.get(Properties.class, Jpa.class))));


  }
}