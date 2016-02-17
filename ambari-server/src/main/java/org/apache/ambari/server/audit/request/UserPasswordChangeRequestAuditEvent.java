/*
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

package org.apache.ambari.server.audit.request;

public class UserPasswordChangeRequestAuditEvent extends RequestAuditEvent {

  public static class UserPasswordChangeRequestAuditEventBuilder extends RequestAuditEventBuilder<UserPasswordChangeRequestAuditEvent, UserPasswordChangeRequestAuditEventBuilder> {

    private String username;

    public UserPasswordChangeRequestAuditEventBuilder() {
      super.withOperation("Password change");
    }

    @Override
    protected UserPasswordChangeRequestAuditEvent newAuditEvent() {
      return new UserPasswordChangeRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Affected username(")
        .append(username)
        .append(")");
    }


    public UserPasswordChangeRequestAuditEventBuilder withAffectedUsername(String username) {
      this.username = username;
      return this;
    }
  }

  protected UserPasswordChangeRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected UserPasswordChangeRequestAuditEvent(UserPasswordChangeRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link UserPasswordChangeRequestAuditEvent}
   * @return a builder instance
   */
  public static UserPasswordChangeRequestAuditEventBuilder builder() {
    return new UserPasswordChangeRequestAuditEventBuilder();
  }

}
