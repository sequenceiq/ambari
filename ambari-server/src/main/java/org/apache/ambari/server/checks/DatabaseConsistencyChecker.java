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
package org.apache.ambari.server.checks;

import java.util.Enumeration;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.log4j.FileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.Log4jLoggerAdapter;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.AmbariJpaPersistModule;

@Singleton
public class DatabaseConsistencyChecker {

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseConsistencyChecker.class);

  @Inject
  private PersistService persistService;

  @Inject
  private DatabaseConsistencyCheckHelper databaseConsistencyCheckHelper;

  public DatabaseConsistencyChecker() {
  }

  public void performDatabaseConsistencyChecks() {
    LOG.info("Starting database consistency checks ...");
    persistService.start();

    databaseConsistencyCheckHelper.checkForNotMappedConfigsToCluster();

    databaseConsistencyCheckHelper.checkForConfigsSelectedMoreThanOnce();

    databaseConsistencyCheckHelper.checkForHostsWithoutState();

    databaseConsistencyCheckHelper.checkHostComponentStatesCountEqualsHostComponentsDesiredStates();

    databaseConsistencyCheckHelper.checkServiceConfigs();

    persistService.stop();
    LOG.info("Finished database consistency checks.");
  }


  /*
  * Main method from which we are calling all checks
  * */
  public static void main(String[] args) throws Exception {
    DatabaseConsistencyChecker databaseConsistencyChecker = null;
    try {
      LOG.info("******************************* Check database started *******************************");

      Injector injector = Guice.createInjector(new AmbariCoreModule(), new DatabaseCheckModule(), new AmbariJpaPersistModule("ambari-server"), new AuditLoggerModule());
      databaseConsistencyChecker = injector.getInstance(DatabaseConsistencyChecker.class);
      databaseConsistencyChecker.performDatabaseConsistencyChecks();


      LOG.info("******************************* Check database completed *******************************");
    } catch (Throwable e) {
      if (e instanceof AmbariException) {
        LOG.error("Exception occurred during database check:", e);
        throw (AmbariException) e;
      } else {
        LOG.error("Unexpected error, database check failed", e);
        throw new Exception("Unexpected error, database check failed", e);
      }
    } finally {
      if (DatabaseConsistencyCheckHelper.isErrorAvailable()) {
        String ambariDBConsistencyCheckLog = "ambari-server-check-database.log";
        if (LOG instanceof Log4jLoggerAdapter) {
          org.apache.log4j.Logger dbConsistencyCheckHelperLogger = org.apache.log4j.Logger.getLogger(DatabaseConsistencyCheckHelper.class);
          Enumeration appenders = dbConsistencyCheckHelperLogger.getAllAppenders();
          while (appenders.hasMoreElements()) {
            Object appender = appenders.nextElement();
            if (appender instanceof FileAppender) {
              ambariDBConsistencyCheckLog = ((FileAppender) appender).getFile();
              break;
            }
          }
        }
        ambariDBConsistencyCheckLog = ambariDBConsistencyCheckLog.replace("//", "/");
        System.out.print(String.format("DB configs consistency check failed. Run \"ambari-server start --skip-database-check\" to skip. " +
            "If you use this \"--skip-database-check\" option, do not make any changes to your cluster topology " +
            "or perform a cluster upgrade until you correct the database consistency issues. See \"%s\" " +
            "for more details on the consistency issues.", ambariDBConsistencyCheckLog));
      } else {
        System.out.print("No errors were found.");
      }

    }
  }


}
