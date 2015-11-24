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

import org.eclipse.persistence.indirection.IndirectSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class AlertTargetEntityTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AlertTargetEntityTest.class);
  private AlertTargetEntity alertTargetEntity;
  private Random rnd = new Random();

  @Before
  public void setUp() throws Exception {
    // the instance being tested
    alertTargetEntity = new AlertTargetEntity();

    // generating 5 alert groups linked to this target
    Set<AlertGroupEntity> initial = new IndirectSet(generateAlertGroupSetFromNames(generateAlerGroupNamesList(5)));
    for (AlertGroupEntity age : initial) {
      age.addAlertTarget(alertTargetEntity);
    }

  }

  @Test
  public void testShouldThrowConcurrentModificationException() {

    // GIVEN
    //the new set of alertGroups to be set
    Set<AlertGroupEntity> tiktakk = generateAlertGroupSetFromNames(generateAlerGroupNamesList(10));

    // WHEN
    alertTargetEntity.setAlertGroups(tiktakk);

    // THEN

    Assert.assertEquals("Alert groups haven't been changed as expected", alertTargetEntity.getAlertGroups().size(),
        tiktakk.size());

  }


  private Set<AlertGroupEntity> generateAlertGroupSetFromNames(List<String> alertGroupNames) {

    Set<AlertGroupEntity> alertGroupEntitySet = new HashSet<>();

    for (String name : alertGroupNames) {
      AlertGroupEntity age = new AlertGroupEntity();
      age.setGroupName(name);
      age.setGroupId(Long.valueOf(name.split("-")[1]));
      alertGroupEntitySet.add(age);
    }
    return alertGroupEntitySet;
  }


  private String generateAlertGroupName() {
    String s = "AlertGroup-" + Math.abs(rnd.nextInt());
    LOGGER.info("generated: {}", s);
    return s;
  }

  private List<String> generateAlerGroupNamesList(int nbr) {
    List<String> ret = new ArrayList<>();
    for (int i = 0; i <= nbr; i++) {
      ret.add(generateAlertGroupName());
    }
    return ret;
  }


}