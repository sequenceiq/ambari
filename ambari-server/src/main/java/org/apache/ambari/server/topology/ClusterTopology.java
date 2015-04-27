/**
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

package org.apache.ambari.server.topology;

import java.util.Collection;
import java.util.Map;

/**
 * Represents a full cluster topology including all instance information as well as the associated
 * blueprint which provides all abstract topology information.
 */
public interface ClusterTopology {

  /**
   * Get the name of the cluster.
   *
   * @return cluster name
   */
  public String getClusterName();

  /**
   * Get the blueprint associated with the cluster.
   *
   * @return assocaited blueprint
   */
  public Blueprint getBlueprint();

  /**
   * Get the cluster scoped configuration for the cluster.
   * This configuration has the blueprint cluster scoped
   * configuration set as it's parent.
   *
   * @return cluster scoped configuration
   */
  public Configuration getConfiguration();

  /**
   * Get host group information.
   *
   * @return map of host group name to host group information
   */
  public Map<String, HostGroupInfo> getHostGroupInfo();

  /**
   * Get the names of  all of host groups which contain the specified component.
   *
   * @param component  component name
   *
   * @return collection of host group names which contain the specified component
   */
  public Collection<String> getHostGroupsForComponent(String component);

  /**
   * Get the name of the host group which is mapped to the specified host.
   *
   * @param hostname  host name
   *
   * @return name of the host group which is mapped to the specified host or null if
   *         no group is mapped to the host
   */
  public String getHostGroupForHost(String hostname);

  /**
   * Get all hosts which are mapped to a host group which contains the specified component.
   * The host need only to be mapped to the hostgroup, not actually provisioned.
   *
   * @param component  component name
   *
   * @return collection of hosts for the specified component; will not return null
   */
  public Collection<String> getHostAssignmentsForComponent(String component);

  /**
   * Update the existing topology based on the provided topology request.
   *
   * @param topologyRequest  request modifying the topology
   *
   * @throws InvalidTopologyException if the request specified invalid topology information or if
   *                                  making the requested changes would result in an invalid topology
   */
  public void update(TopologyRequest topologyRequest) throws InvalidTopologyException;

  /**
   * Add a new host to the topology.
   *
   * @param hostGroupName  name of associated host group
   * @param host           name of host
   *
   * @throws InvalidTopologyException if the host being added is already registered to a different host group
   * @throws NoSuchHostGroupException if the specified host group is invalid
   */
  public void addHostToTopology(String hostGroupName, String host) throws InvalidTopologyException, NoSuchHostGroupException;

  /**
   * Determine if NameNode HA is enabled.
   *
   * @return true if NameNode HA is enabled; false otherwise
   */
  public boolean isNameNodeHAEnabled();
}
