#!/usr/bin/env python

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


import logging
import copy
import time
import threading
import pprint

from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.LiveStatus import LiveStatus


logger = logging.getLogger()

"""
RecoveryManager has the following capabilities:
* Store data needed for execution commands extracted from STATUS command
* Generate INSTALL command
* Generate START command
"""


class RecoveryManager:
  COMMAND_TYPE = "commandType"
  PAYLOAD_LEVEL = "payloadLevel"
  COMPONENT_NAME = "componentName"
  ROLE = "role"
  TASK_ID = "taskId"
  DESIRED_STATE = "desiredState"
  HAS_STALE_CONFIG = "hasStaleConfigs"
  EXECUTION_COMMAND_DETAILS = "executionCommandDetails"
  ROLE_COMMAND = "roleCommand"
  PAYLOAD_LEVEL_DEFAULT = "DEFAULT"
  PAYLOAD_LEVEL_MINIMAL = "MINIMAL"
  PAYLOAD_LEVEL_EXECUTION_COMMAND = "EXECUTION_COMMAND"
  STARTED = "STARTED"
  INSTALLED = "INSTALLED"
  INIT = "INIT"  # TODO: What is the state when machine is reset
  COMPONENT_UPDATE_KEY_FORMAT = "{0}_UPDATE_TIME"
  COMMAND_REFRESH_DELAY_SEC = 600 #10 minutes

  default_action_counter = {
    "lastAttempt": 0,
    "count": 0,
    "lastReset": 0,
    "lifetimeCount" : 0,
    "warnedLastAttempt": False,
    "warnedLastReset": False,
    "warnedThresholdReached": False
  }

  default_component_status = {
    "current": "",
    "desired": "",
    "stale_config": False
  }


  def __init__(self, recovery_enabled=False, auto_start_only=False):
    self.recovery_enabled = recovery_enabled
    self.auto_start_only = auto_start_only
    self.max_count = 6
    self.window_in_min = 60
    self.retry_gap = 5
    self.max_lifetime_count = 12

    self.stored_exec_commands = {}
    self.id = int(time.time())
    self.allowed_desired_states = [self.STARTED, self.INSTALLED]
    self.allowed_current_states = [self.INIT, self.INSTALLED]
    self.actions = {}
    self.statuses = {}
    self.__status_lock = threading.RLock()
    self.__command_lock = threading.RLock()
    self.__active_command_lock = threading.RLock()
    self.active_command_count = 0
    self.paused = False

    self.update_config(6, 60, 5, 12, recovery_enabled, auto_start_only)

    pass

  def start_execution_command(self):
    with self.__active_command_lock:
      self.active_command_count += 1
    pass

  def stop_execution_command(self):
    with self.__active_command_lock:
      self.active_command_count -= 1
    pass

  def has_active_command(self):
    return self.active_command_count > 0

  def set_paused(self, paused):
    if self.paused != paused:
      logger.debug("RecoveryManager is transitioning from isPaused = " + str(self.paused) + " to " + str(paused))
    self.paused = paused

  def enabled(self):
    return self.recovery_enabled


  def update_config_staleness(self, component, is_config_stale):
    """
    Updates staleness of config
    """
    if component not in self.statuses:
      self.__status_lock.acquire()
      try:
        if component not in self.statuses:
          self.statuses[component] = copy.deepcopy(self.default_component_status)
      finally:
        self.__status_lock.release()
      pass

    self.statuses[component]["stale_config"] = is_config_stale
    if self.statuses[component]["current"] == self.statuses[component]["desired"] and \
            self.statuses[component]["stale_config"] == False:
      self.remove_command(component)
    pass

  def update_current_status(self, component, state):
    """
    Updates the current status of a host component managed by the agent
    """
    if component not in self.statuses:
      self.__status_lock.acquire()
      try:
        if component not in self.statuses:
          self.statuses[component] = copy.deepcopy(self.default_component_status)
      finally:
        self.__status_lock.release()
      pass

    self.statuses[component]["current"] = state
    if self.statuses[component]["current"] == self.statuses[component]["desired"] and \
            self.statuses[component]["stale_config"] == False:
      self.remove_command(component)
    pass


  def update_desired_status(self, component, state):
    """
    Updates the desired status of a host component managed by the agent
    """
    if component not in self.statuses:
      self.__status_lock.acquire()
      try:
        if component not in self.statuses:
          self.statuses[component] = copy.deepcopy(self.default_component_status)
      finally:
        self.__status_lock.release()
      pass

    self.statuses[component]["desired"] = state
    if self.statuses[component]["current"] == self.statuses[component]["desired"] and \
            self.statuses[component]["stale_config"] == False:
      self.remove_command(component)
    pass


  def requires_recovery(self, component):
    """
    Recovery is allowed for:
    INISTALLED --> STARTED
    INIT --> INSTALLED --> STARTED
    RE-INSTALLED (if configs do not match)
    """
    if not self.enabled():
      return False

    if component not in self.statuses:
      return False

    if self.auto_start_only:
      status = self.statuses[component]
      if status["current"] == status["desired"]:
        return False
    else:
      status = self.statuses[component]
      if status["current"] == status["desired"] and status['stale_config'] == False:
        return False

    if status["desired"] not in self.allowed_desired_states or status["current"] not in self.allowed_current_states:
      return False

    logger.info("%s needs recovery.", component)
    return True
    pass



  def get_recovery_status(self):
    """
    Creates a status in the form
    {
      "summary" : "RECOVERABLE|DISABLED|PARTIALLY_RECOVERABLE|UNRECOVERABLE",
      "component_reports" : [
        {
          "name": "component_name",
          "numAttempts" : "x",
          "limitReached" : "true|false"
          "status" : "REQUIRES_RECOVERY|RECOVERY_COMMAND_REQUESTED|RECOVERY_COMMAND_ISSUED|NO_RECOVERY_NEEDED"
        }
      ]
    }
    """
    report = {}
    report["summary"] = "DISABLED"
    if self.enabled():
      report["summary"] = "RECOVERABLE"
      num_limits_reached = 0
      recovery_states = []
      report["componentReports"] = recovery_states
      self.__status_lock.acquire()
      try:
        for component in self.actions.keys():
          action = self.actions[component]
          recovery_state = {}
          recovery_state["name"] = component
          recovery_state["numAttempts"] = action["lifetimeCount"]
          recovery_state["limitReached"] = self.max_lifetime_count <= action["lifetimeCount"]
          recovery_states.append(recovery_state)
          if recovery_state["limitReached"] == True:
            num_limits_reached += 1
          pass
      finally:
        self.__status_lock.release()

      if num_limits_reached > 0:
        report["summary"] = "PARTIALLY_RECOVERABLE"
        if num_limits_reached == len(recovery_states):
          report["summary"] = "UNRECOVERABLE"

    return report
    pass

  def get_recovery_commands(self):
    """
    This method computes the recovery commands for the following transitions
    INSTALLED --> STARTED
    INIT --> INSTALLED
    """
    commands = []
    for component in self.statuses.keys():
      if self.requires_recovery(component) and self.may_execute(component):
        status = copy.deepcopy(self.statuses[component])
        command = None
        if self.auto_start_only:
          if status["desired"] == self.STARTED:
            if status["current"] == self.INSTALLED:
              command = self.get_start_command(component)
        else:
          # START, INSTALL, RESTART
          if status["desired"] != status["current"]:
            if status["desired"] == self.STARTED:
              if status["current"] == self.INSTALLED:
                command = self.get_start_command(component)
              elif status["current"] == self.INIT:
                command = self.get_install_command(component)
            elif status["desired"] == self.INSTALLED:
              if status["current"] == self.INIT:
                command = self.get_install_command(component)
              # else issue a STOP command
          else:
            if status["current"] == self.INSTALLED:
              command = self.get_install_command(component)
            elif status["current"] == self.STARTED:
              command = self.get_restart_command(component)

        if command:
          self.execute(component)
          commands.append(command)
    return commands
    pass


  def may_execute(self, action):
    """
    Check if an action can be executed
    """
    if not action or action.strip() == "":
      return False

    if action not in self.actions:
      self.__status_lock.acquire()
      try:
        self.actions[action] = copy.deepcopy(self.default_action_counter)
      finally:
        self.__status_lock.release()
    return self._execute_action_chk_only(action)
    pass


  def execute(self, action):
    """
    Executed an action
    """
    if not action or action.strip() == "":
      return False

    if action not in self.actions:
      self.__status_lock.acquire()
      try:
        self.actions[action] = copy.deepcopy(self.default_action_counter)
      finally:
        self.__status_lock.release()
    return self._execute_action_(action)
    pass


  def _execute_action_(self, action_name):
    """
    _private_ implementation of [may] execute
    """
    action_counter = self.actions[action_name]
    now = self._now_()
    seconds_since_last_attempt = now - action_counter["lastAttempt"]
    if action_counter["lifetimeCount"] < self.max_lifetime_count:
      if action_counter["count"] < self.max_count:
        if seconds_since_last_attempt > self.retry_gap_in_sec:
          action_counter["count"] += 1
          action_counter["lifetimeCount"] +=1
          if self.retry_gap > 0:
            action_counter["lastAttempt"] = now
          action_counter["warnedLastAttempt"] = False
          if action_counter["count"] == 1:
            action_counter["lastReset"] = now
          return True
        else:
          if action_counter["warnedLastAttempt"] == False:
            action_counter["warnedLastAttempt"] = True
            logger.warn(
              "%s seconds has not passed since last occurrence %s seconds back for %s. " +
              "Will silently skip execution without warning till retry gap is passed",
              self.retry_gap_in_sec, seconds_since_last_attempt, action_name)
          else:
            logger.debug(
              "%s seconds has not passed since last occurrence %s seconds back for %s",
              self.retry_gap_in_sec, seconds_since_last_attempt, action_name)
      else:
        sec_since_last_reset = now - action_counter["lastReset"]
        if sec_since_last_reset > self.window_in_sec:
          action_counter["count"] = 1
          action_counter["lifetimeCount"] +=1
          if self.retry_gap > 0:
            action_counter["lastAttempt"] = now
          action_counter["lastReset"] = now
          action_counter["warnedLastReset"] = False
          return True
        else:
          if action_counter["warnedLastReset"] == False:
            action_counter["warnedLastReset"] = True
            logger.warn("%s occurrences in %s minutes reached the limit for %s. " +
                        "Will silently skip execution without warning till window is reset",
                        action_counter["count"], self.window_in_min, action_name)
          else:
            logger.debug("%s occurrences in %s minutes reached the limit for %s",
                         action_counter["count"], self.window_in_min, action_name)
    else:
      if action_counter["warnedThresholdReached"] == False:
        action_counter["warnedThresholdReached"] = True
        logger.warn("%s occurrences in agent life time reached the limit for %s. " +
                    "Will silently skip execution without warning till window is reset",
                    action_counter["lifetimeCount"], action_name)
      else:
        logger.debug("%s occurrences in agent life time reached the limit for %s",
                     action_counter["lifetimeCount"], action_name)
    return False
    pass


  def _execute_action_chk_only(self, action_name):
    """
    _private_ implementation of [may] execute check only
    """
    action_counter = self.actions[action_name]
    now = self._now_()
    seconds_since_last_attempt = now - action_counter["lastAttempt"]

    if action_counter["lifetimeCount"] < self.max_lifetime_count:
      if action_counter["count"] < self.max_count:
        if seconds_since_last_attempt > self.retry_gap_in_sec:
          return True
      else:
        sec_since_last_reset = now - action_counter["lastReset"]
        if sec_since_last_reset > self.window_in_sec:
          return True

    return False
    pass

  def _now_(self):
    return int(time.time())
    pass


  def update_configuration_from_registration(self, reg_resp):
    """
    TODO: Server sends the recovery configuration - call update_config after parsing
    "recovery_config": {
      "type" : "DEFAULT|AUTO_START|FULL",
      "maxCount" : 10,
      "windowInMinutes" : 60,
      "retryGap" : 0 }
    """

    recovery_enabled = False
    auto_start_only = True
    max_count = 6
    window_in_min = 60
    retry_gap = 5
    max_lifetime_count = 12

    if reg_resp and "recoveryConfig" in reg_resp:
      config = reg_resp["recoveryConfig"]
      if "type" in config:
        if config["type"] in ["AUTO_START", "FULL"]:
          recovery_enabled = True
          if config["type"] == "FULL":
            auto_start_only = False
      if "maxCount" in config:
        max_count = self._read_int_(config["maxCount"], max_count)
      if "windowInMinutes" in config:
        window_in_min = self._read_int_(config["windowInMinutes"], window_in_min)
      if "retryGap" in config:
        retry_gap = self._read_int_(config["retryGap"], retry_gap)
      if 'maxLifetimeCount' in config:
        max_lifetime_count = self._read_int_(config['maxLifetimeCount'], max_lifetime_count)
    self.update_config(max_count, window_in_min, retry_gap, max_lifetime_count, recovery_enabled, auto_start_only)
    pass


  def update_config(self, max_count, window_in_min, retry_gap, max_lifetime_count, recovery_enabled, auto_start_only):
    """
    Update recovery configuration, recovery is disabled if configuration values
    are not correct
    """
    self.recovery_enabled = False;
    if max_count <= 0:
      logger.warn("Recovery disabled: max_count must be a non-negative number")
      return

    if window_in_min <= 0:
      logger.warn("Recovery disabled: window_in_min must be a non-negative number")
      return

    if retry_gap < 1:
      logger.warn("Recovery disabled: retry_gap must be a positive number and at least 1")
      return
    if retry_gap >= window_in_min:
      logger.warn("Recovery disabled: retry_gap must be smaller than window_in_min")
      return
    if max_lifetime_count < 0 or max_lifetime_count < max_count:
      logger.warn("Recovery disabled: max_lifetime_count must more than 0 and >= max_count")
      return

    self.max_count = max_count
    self.window_in_min = window_in_min
    self.retry_gap = retry_gap
    self.window_in_sec = window_in_min * 60
    self.retry_gap_in_sec = retry_gap * 60
    self.auto_start_only = auto_start_only
    self.max_lifetime_count = max_lifetime_count

    self.allowed_desired_states = [self.STARTED, self.INSTALLED]
    self.allowed_current_states = [self.INIT, self.INSTALLED, self.STARTED]

    if self.auto_start_only:
      self.allowed_desired_states = [self.STARTED]
      self.allowed_current_states = [self.INSTALLED]

    self.recovery_enabled = recovery_enabled
    if self.recovery_enabled:
      logger.info(
        "==> Auto recovery is enabled with maximum %s in %s minutes with gap of %s minutes between and lifetime max being %s.",
        self.max_count, self.window_in_min, self.retry_gap, self.max_lifetime_count)
    pass


  def get_unique_task_id(self):
    self.id += 1
    return self.id
    pass


  def process_status_commands(self, commands):
    if not self.enabled():
      return

    if commands and len(commands) > 0:
      for command in commands:
        self.store_or_update_command(command)
        if self.EXECUTION_COMMAND_DETAILS in command:
          logger.debug("Details to construct exec commands: " + pprint.pformat(command[self.EXECUTION_COMMAND_DETAILS]))

    pass


  def process_execution_commands(self, commands):
    if not self.enabled():
      return

    if commands and len(commands) > 0:
      for command in commands:
        if self.COMMAND_TYPE in command and command[self.COMMAND_TYPE] == ActionQueue.EXECUTION_COMMAND:
          if self.ROLE in command:
            if command[self.ROLE_COMMAND] == ActionQueue.ROLE_COMMAND_INSTALL:
              self.update_desired_status(command[self.ROLE], LiveStatus.DEAD_STATUS)
            if command[self.ROLE_COMMAND] == ActionQueue.ROLE_COMMAND_START:
              self.update_desired_status(command[self.ROLE], LiveStatus.LIVE_STATUS)
    pass


  def store_or_update_command(self, command):
    """
    Stores command details by reading them from the STATUS_COMMAND
    Update desired state as well
    """
    if not self.enabled():
      return

    logger.debug("Inspecting command to store/update details")
    if self.COMMAND_TYPE in command and command[self.COMMAND_TYPE] == ActionQueue.STATUS_COMMAND:
      payloadLevel = self.PAYLOAD_LEVEL_DEFAULT
      if self.PAYLOAD_LEVEL in command:
        payloadLevel = command[self.PAYLOAD_LEVEL]

      component = command[self.COMPONENT_NAME]
      self.update_desired_status(component, command[self.DESIRED_STATE])
      self.update_config_staleness(component, command[self.HAS_STALE_CONFIG])

      if payloadLevel == self.PAYLOAD_LEVEL_EXECUTION_COMMAND:
        if self.EXECUTION_COMMAND_DETAILS in command:
          # Store the execution command details
          self.remove_command(component)
          self.add_command(component, command[self.EXECUTION_COMMAND_DETAILS])
          logger.debug("Stored command details for " + component)
        else:
          logger.warn("Expected field " + self.EXECUTION_COMMAND_DETAILS + " unavailable.")
        pass
    pass


  def get_install_command(self, component):
    if self.paused:
      logger.info("Recovery is paused, likely tasks waiting in pipeline for this host.")
      return None

    if self.enabled():
      logger.debug("Using stored INSTALL command for %s", component)
      if self.command_exists(component, ActionQueue.EXECUTION_COMMAND):
        command = copy.deepcopy(self.stored_exec_commands[component])
        command[self.ROLE_COMMAND] = "INSTALL"
        command[self.COMMAND_TYPE] = ActionQueue.AUTO_EXECUTION_COMMAND
        command[self.TASK_ID] = self.get_unique_task_id()
        return command
      else:
        logger.info("INSTALL command cannot be computed as details are not received from Server.")
    else:
      logger.info("Recovery is not enabled. INSTALL command will not be computed.")
    return None
    pass

  def get_restart_command(self, component):
    if self.paused:
      logger.info("Recovery is paused, likely tasks waiting in pipeline for this host.")
      return None

    if self.enabled():
      logger.debug("Using stored INSTALL command for %s", component)
      if self.command_exists(component, ActionQueue.EXECUTION_COMMAND):
        command = copy.deepcopy(self.stored_exec_commands[component])
        command[self.ROLE_COMMAND] = "CUSTOM_COMMAND"
        command[self.COMMAND_TYPE] = ActionQueue.AUTO_EXECUTION_COMMAND
        command[self.TASK_ID] = self.get_unique_task_id()
        command['hostLevelParams']['custom_command'] = 'RESTART'
        return command
      else:
        logger.info("RESTART command cannot be computed as details are not received from Server.")
    else:
      logger.info("Recovery is not enabled. RESTART command will not be computed.")
    return None
    pass


  def get_start_command(self, component):
    if self.paused:
      logger.info("Recovery is paused, likely tasks waiting in pipeline for this host.")
      return None

    if self.enabled():
      logger.debug("Using stored START command for %s", component)
      if self.command_exists(component, ActionQueue.EXECUTION_COMMAND):
        command = copy.deepcopy(self.stored_exec_commands[component])
        command[self.ROLE_COMMAND] = "START"
        command[self.COMMAND_TYPE] = ActionQueue.AUTO_EXECUTION_COMMAND
        command[self.TASK_ID] = self.get_unique_task_id()
        return command
      else:
        logger.info("START command cannot be computed as details are not received from Server.")
    else:
      logger.info("Recovery is not enabled. START command will not be computed.")

    return None
    pass


  def command_exists(self, component, command_type):
    if command_type == ActionQueue.EXECUTION_COMMAND:
      self.remove_stale_command(component)
      if component in self.stored_exec_commands:
        return True

    return False
    pass


  def remove_stale_command(self, component):
    component_update_key = self.COMPONENT_UPDATE_KEY_FORMAT.format(component)
    if component in self.stored_exec_commands:
      insert_time = self.stored_exec_commands[component_update_key]
      age = self._now_() - insert_time
      if self.COMMAND_REFRESH_DELAY_SEC < age:
        logger.debug("Removing stored command for component : " + str(component) + " as its " + str(age) + " sec old")
        self.remove_command(component)
    pass


  def remove_command(self, component):
    if component in self.stored_exec_commands:
      self.__status_lock.acquire()
      try:
        component_update_key = self.COMPONENT_UPDATE_KEY_FORMAT.format(component)
        del self.stored_exec_commands[component]
        del self.stored_exec_commands[component_update_key]
        logger.debug("Removed stored command for component : " + str(component))
        return True
      finally:
        self.__status_lock.release()
    return False


  def add_command(self, component, command):
    self.__status_lock.acquire()
    try:
      component_update_key = self.COMPONENT_UPDATE_KEY_FORMAT.format(component)
      self.stored_exec_commands[component] = command
      self.stored_exec_commands[component_update_key] = self._now_()
      logger.debug("Added command for component : " + str(component))
    finally:
      self.__status_lock.release()


  def _read_int_(self, value, default_value=0):
    int_value = default_value
    try:
      int_value = int(value)
    except ValueError:
      pass
    return int_value


def main(argv=None):
  cmd_mgr = RecoveryManager()
  pass


if __name__ == '__main__':
  main()