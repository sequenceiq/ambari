/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.orm.entities;

import javax.persistence.Cacheable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.PreRemove;
import javax.persistence.Table;

@Entity
@Table(name = "alert_group_target")
@Cacheable(false)
public class AlertGroupTargetEntity {

  @EmbeddedId
  private AlertGroupTargetPK id;

  @MapsId("alertGroup")
  @ManyToOne
  @JoinColumn(name = "group_id", nullable = false)
  private AlertGroupEntity alertGroup;

  @MapsId("alertTarget")
  @ManyToOne
  @JoinColumn(name = "target_id", nullable = false)
  private AlertTargetEntity alertTarget;

  @PreRemove
  protected void remove() {
    // maintaining the relationships
    this.alertGroup.getAlertGroupTargets().remove(this);
    this.alertTarget.getAlertGroupTargets().remove(this);
  }

  public AlertGroupTargetEntity() {
  }

  public AlertGroupTargetEntity(AlertGroupEntity alertGroup, AlertTargetEntity alertTarget) {
    this.alertGroup = alertGroup;
    this.alertTarget = alertTarget;
  }

  public AlertGroupEntity getAlertGroup() {
    return alertGroup;
  }


  public AlertTargetEntity getAlertTarget() {
    return alertTarget;
  }



}
