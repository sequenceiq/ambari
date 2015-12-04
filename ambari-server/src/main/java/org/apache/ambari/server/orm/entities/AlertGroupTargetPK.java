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

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class AlertGroupTargetPK implements Serializable {
  private Long alertTarget;
  private Long alertGroup;

  public Long getAlertTarget() {
    return alertTarget;
  }

  public void setAlertTarget(Long alertTarget) {
    this.alertTarget = alertTarget;
  }

  public Long getAlertGroup() {
    return alertGroup;
  }

  public void setAlertGroup(Long alertGroup) {
    this.alertGroup = alertGroup;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AlertGroupTargetPK that = (AlertGroupTargetPK) o;

    if (!alertTarget.equals(that.alertTarget)) return false;
    return alertGroup.equals(that.alertGroup);

  }

  @Override
  public int hashCode() {
    int result = alertTarget.hashCode();
    result = 31 * result + alertGroup.hashCode();
    return result;
  }
}
