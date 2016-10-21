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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.hooks.AmbariEventFactory;
import org.apache.ambari.server.hooks.HookContext;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.serveraction.users.PostUserCreationHookServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Service in charge for handling user initialization related logic.
 * It's expected that this implementation encapsulates all the logic around the user initialization hook:
 * 1. validates the context  (all the input is available)
 * 2. checks if prerequisites are satisfied for the hook execution
 * 3. triggers the hook execution flow
 * 4. executes the flow (on a separate thread)
 */
@Singleton
public class UserHookService implements HookService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserHookService.class);

  @Inject
  private AmbariEventFactory eventFactory;

  @Inject
  private AmbariEventPublisher ambariEventPublisher;

  @Inject
  private ActionManager actionManager;

  @Inject
  private RequestFactory requestFactory;

  @Inject
  private StageFactory stageFactory;

  @Inject
  private Configuration configuration;

  @Inject
  private Clusters clusters;

  @Inject
  private ObjectMapper objectMapper;

  private static final String BASE_LOG_DIR = "/tmp/ambari";
  private static final String USER_INIT_REQUEST_CONTEXT = "Post user creation hook for [ %s ] users";


  // executed by the IoC framework after creating the object (guice)
  @Inject
  private void register() {
    ambariEventPublisher.register(this);
  }

  @Override
  public void execute(HookContext hookContext) {
    LOGGER.info("Executing user hook for {}. ", hookContext);

    BatchUserHookContext hookCtx = validateHookInput(hookContext);

    if (!checkUserHookPrerequisites()) {
      LOGGER.warn("Prerequisites for user hook are not satisfied. Hook not triggered");
      return;
    }

    if (hookCtx.getUserGroups().isEmpty()) {
      LOGGER.info("No users found for executing user hook for");
    }

    UserCreatedEvent userCreatedEvent = (UserCreatedEvent) eventFactory.newUserCreatedEvent(hookCtx);

    LOGGER.info("Triggering user hook for user: {}", hookContext);
    ambariEventPublisher.publish(userCreatedEvent);
  }

  @Subscribe
  public void onUserInitializationEvent(UserCreatedEvent event) throws AmbariException {
    LOGGER.info("Preparing hook execution for event: {}", event);

    try {
      RequestStageContainer requestStageContainer = new RequestStageContainer(actionManager.getNextRequestId(), null, requestFactory, actionManager);
      ClusterData clsData = getClusterNameAndId();

      BatchUserHookContext ctx = (BatchUserHookContext) event.getContext();

      String stageContextText = String.format(USER_INIT_REQUEST_CONTEXT, ctx.getUserGroups().size());

      Stage stage = stageFactory.createNew(requestStageContainer.getId(), BASE_LOG_DIR + File.pathSeparator + requestStageContainer.getId(), clsData.getClusterName(),
          clsData.getClusterId(), stageContextText, "{}", "{}", "{}");
      stage.setStageId(requestStageContainer.getLastStageId());

      ServiceComponentHostServerActionEvent serverActionEvent = new ServiceComponentHostServerActionEvent("ambari-server-host", System.currentTimeMillis());
      Map<String, String> commandParams = prepareCommandParams(ctx, clsData);

      // todo what's the user here?
      stage.addServerActionCommand(PostUserCreationHookServerAction.class.getName(), "ambari", Role.AMBARI_SERVER_ACTION,
          RoleCommand.EXECUTE, clsData.getClusterName(), serverActionEvent, commandParams, stageContextText, null, null, false, false);

      requestStageContainer.addStages(Collections.singletonList(stage));
      requestStageContainer.persist();

    } catch (IOException e) {
      LOGGER.error("Failed to assemble stage for server action. Event: {}", event);
      throw new AmbariException("Failed to assemble stage for server action", e);
    }

  }

  private Map<String, String> prepareCommandParams(BatchUserHookContext context, ClusterData clusterData) throws IOException {
    Map<String, String> commandParams = new HashMap<>();
    commandParams.put(UserHookParams.SCRIPT.param(), configuration.getProperty(Configuration.POST_USER_CREATION_HOOK));
    // commandparam passed as a json string to be handled in the action implementaiton
    commandParams.put(UserHookParams.PAYLOAD.param(), objectMapper.writeValueAsString(context.getUserGroups()));
    commandParams.put(UserHookParams.CLUSTER_ID.param(), String.valueOf(clusterData.getClusterId()));
    commandParams.put(UserHookParams.CLUSTER_NAME.param(), clusterData.getClusterName());

    return commandParams;
  }

  private boolean checkUserHookPrerequisites() {

    if (!configuration.isUserHookEnabled()) {
      LOGGER.warn("User hook disabled.");
      return false;
    }

    if (clusters.getClusters().isEmpty()) {
      LOGGER.warn("There's no cluster found. User hook won't be executed.");
      return false;
    }

    return true;
  }

  private BatchUserHookContext validateHookInput(HookContext hookContext) {
    // perform any other validation steps, such as existence of fields etc...
    return (BatchUserHookContext) hookContext;
  }

  private ClusterData getClusterNameAndId() {
    // cluster data is needed multiple times during the stage creation, cached it locally ...
    Map.Entry<String, Cluster> clustersMapEntry = clusters.getClusters().entrySet().iterator().next();
    return new ClusterData(clustersMapEntry.getKey(), clustersMapEntry.getValue().getClusterId());
  }

  /**
   * Local representation of cluster data.
   */
  private static final class ClusterData {
    private String clusterName;
    private Long clusterId;

    public ClusterData(String clusterName, Long clusterId) {
      this.clusterName = clusterName;
      this.clusterId = clusterId;
    }

    public String getClusterName() {
      return clusterName;
    }

    public Long getClusterId() {
      return clusterId;
    }
  }

}
