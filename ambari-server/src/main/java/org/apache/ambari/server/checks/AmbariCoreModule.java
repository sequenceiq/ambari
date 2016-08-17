/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.ambari.server.checks;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * Core amabari-server configuration module.
 *
 * Configure generic bindings here that are the backbone of the application.
 * (eg.: configuration entries, constants etc...)
 */
public class AmbariCoreModule extends AbstractModule {

  private Configuration configuration;

  @Override
  protected void configure() {
    // load and bind the configuration (this should only happen once in the application's lifetime)
    // the instance is not ccreated by the framework, so injections won't be honored!
    configuration = new Configuration();
    bind(Configuration.class).toInstance(configuration);
    bind(OsFamily.class);

    // named constants can be bound here

    // the constant is bound to a configuration value!
    bindConstant().annotatedWith(Names.named("sharedResourcesDirPath")).to(configuration.getSharedResourcesDirPath());
  }
}
