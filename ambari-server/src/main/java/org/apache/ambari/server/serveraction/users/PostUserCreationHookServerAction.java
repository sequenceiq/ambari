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

package org.apache.ambari.server.serveraction.users;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.hooks.users.UserHookParams;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PostUserCreationHookServerAction extends AbstractServerAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(PostUserCreationHookServerAction.class);

  private static final String USR_BIN_ENV = "/usr/bin/env";
  private static final String BASH = "bash";

  @Inject
  private ShellCommandUtilityWrapper shellCommandUtilityWrapper;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  public PostUserCreationHookServerAction() {
    super();
  }

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
    LOGGER.debug("Executing custom script server action; Context: {}", requestSharedDataContext);

    ShellCommandUtil.Result result = null;
    List<CommandReport> commandReports = new ArrayList<>();

    try {
      Map<String, String> commandParams = getCommandParameters();
      validateCommandParams(commandParams);

      List<String> commandBase = Arrays.asList(USR_BIN_ENV, BASH, commandParams.get(UserHookParams.SCRIPT.param()));

      Map<String, List<String>> userGroupMap = getPayload(commandParams);

      for (Map.Entry<String, List<String>> userGroupEntry : userGroupMap.entrySet()) {
        String[] cmd = assembleCommand(commandBase, userGroupEntry, requestSharedDataContext);
        result = shellCommandUtilityWrapper.runCommand(cmd);
        LOGGER.info("Executing command [ {} ] - {}", cmd, result.isSuccessful() ? "succeed" : "failed");

        CommandReport cmdReport = createCommandReport(result.getExitCode(), result.isSuccessful() ? HostRoleStatus.COMPLETED : HostRoleStatus.FAILED, "{}", result.getStdout(), result.getStderr());
        LOGGER.debug("Command report: {}", cmdReport);

        commandReports.add(cmdReport);
      }
    } catch (Exception e) {
      LOGGER.error("Server action is about to quit due to an exception. Number of executed commands: {} ", commandReports.size());
      throw new AmbariException("Failed to execute Server action.", e);
    }

    return aggregateResults(commandReports);
  }


  private String[] assembleCommand(List<String> commandBase, Map.Entry<String, List<String>> userGroupEntry, ConcurrentMap<String, Object> requestSharedDataContext) {
    List<String> cmdList = new ArrayList<>(commandBase);
    cmdList.add(userGroupEntry.getKey());
    for (String group : userGroupEntry.getValue()) {
      cmdList.add(group);
    }
    String[] cmdArray = cmdList.toArray(new String[0]);
    LOGGER.debug("Server action command to execute: {}", cmdArray);

    return cmdArray;
  }

  /**
   * Validates command parameters, throws exception in case required parameters are missing
   */
  private void validateCommandParams(Map<String, String> commandParams) {

    if (!commandParams.containsKey(UserHookParams.PAYLOAD.param())) {
      LOGGER.error("Missing command parameter: {}; Failing the server action.", UserHookParams.PAYLOAD.param());
      throw new IllegalArgumentException("Missing command parameter: [" + UserHookParams.PAYLOAD.param() + "]");
    }

    if (!commandParams.containsKey(UserHookParams.SCRIPT.param())) {
      LOGGER.error("Missing command parameter: {}; Failing the server action.", UserHookParams.SCRIPT.param());
      throw new IllegalArgumentException("Missing command parameter: [" + UserHookParams.SCRIPT.param() + "]");
    }

    LOGGER.info("Command parameter validation passed.");
  }

  private Map<String, List<String>> getPayload(Map<String, String> commandParams) throws IOException {
    Map<String, List<String>> payload = objectMapper.readValue(commandParams.get(UserHookParams.PAYLOAD.param()), Map.class);
    return payload;
  }

  private CommandReport aggregateResults(List<CommandReport> reports) {
    CommandReport commandReport = new CommandReport();

    int exitCode = 0;
    String out = "";
    String err = "";

    for (CommandReport report : reports) {
      exitCode = Math.max(exitCode, report.getExitCode());
      out += report.getStdOut();
      err += report.getStdErr();
    }

    commandReport.setExitCode(exitCode);
    commandReport.setStdOut(out);
    commandReport.setStdErr(err);

    commandReport.setStatus(HostRoleStatus.COMPLETED.toString());

    return commandReport;
  }
}
