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

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;

import junit.framework.Assert;

/**
 * Functional test for testing the IoC container.
 * After building up the context, checks whether the beans in it are set up properly.
 *
 * Assertions are expected to be made on the scope, fields etc... of the beans loaded in the IoC container.
 * As the test operates on the "production" classes (as opposed to mocks), it's a functional test.
 */
public class AmbariCoreModuleFuncionalTest {

  private static Injector injector;

  /**
   * Builds the IoC context for the module being tested.
   * (this only needs to be done once for all the tests in this class)
   */
  @BeforeClass
  public static void beforeClass() {
    injector = Guice.createInjector(new AmbariCoreModule());
  }

  @Test
  public void shouldConfigurationBeBoundToGuiceContextAsSingleton() throws Exception {
    //GIVEN
    // the container is set up

    //WHEN
    Configuration configuration = injector.getInstance(Configuration.class);

    // THEN
    Assert.assertNotNull("Retrieved instance should not be null", configuration);
    Assert.assertTrue("The instance should be singleton", Scopes.isSingleton(injector.getBinding(Configuration.class)));

  }

  @Test
  public void shouldOsFamilyBeBoundToGuiceContextAsSingleton() throws Exception {
    OsFamily osFamily = injector.getInstance(OsFamily.class);
    Assert.assertNotNull("Retrieved instance should not be null", osFamily);
    Assert.assertTrue("The instance should be singleton", Scopes.isSingleton(injector.getBinding(OsFamily.class)));
  }
}