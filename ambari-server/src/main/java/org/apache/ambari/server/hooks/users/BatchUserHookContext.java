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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.hooks.HookContext;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class BatchUserHookContext implements HookContext {
  private Map<String, List<String>> userGroups = new HashMap<>();

  @AssistedInject
  public BatchUserHookContext(@Assisted Map<String, List<String>> userGroups) {
    this.userGroups = userGroups;
  }

  @AssistedInject
  public BatchUserHookContext(@Assisted String userName) {
    userGroups.put(userName, Collections.<String>emptyList());
  }


  public Map<String, List<String>> getUserGroups() {
    return userGroups;
  }
}
