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
package org.apache.ambari.server.cleanup;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.ambari.server.controller.ControllerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.jpa.AmbariJpaPersistService;

/**
 * Class in charge for driving the cleanup process.
 */
public class CleanupDriver {
  private static final Logger LOGGER = LoggerFactory.getLogger(CleanupDriver.class);

  private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
  private static String USAGE = "Usage: java [-cp ...] org.apache.ambari.server.cleanup.CleanupDriver <clusterId> <yyyy-MM-dd>";

  public static void main(String... args) throws Exception {

    try {
      LOGGER.info("Starting the clanup process with arguments:[{}]", args);
      if (args.length != 2) {
        //todo add commons-cli for proper cmd line args handling /  currently this is done by the callee python script
        throw new IllegalArgumentException("ClusterId and fromDate need to be provided!");
      }

      Long clusterId = Long.valueOf(args[0]);
      Date fromdDate = df.parse(args[1]);

      Injector injector = Guice.createInjector(new ControllerModule(), new CleanupModule());
      injector.getInstance(AmbariJpaPersistService.class).start();

      CleanupService cleanupService = injector.getInstance(ChainedCleanupService.class);
      cleanupService.cleanup(CleanupPolicyFactory.timeBasedDeleteCleanupPolicy(fromdDate.getTime(), clusterId));

      LOGGER.info("Cleanup successfully finished!");
    } catch (Exception e) {
      LOGGER.error(USAGE, e);
    }
  }
}
