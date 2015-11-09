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

package org.apache.ambari.server.topology;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

/**
 * Abstract wrapper for Runnable implementations that should run within time and retry constraints.
 * It's intended to be used in cases when the logic implies handling these constraints in the running thread instead of
 * deferring it to a higher level of abstraction.
 */
public abstract class ConstrainedRunnable implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConstrainedRunnable.class);

  private int maxRetryCount = -1;
  private long timeoutIntervalInMillis = -1;

  // todo should this be also coming from config?
  private long sleepTime = 100;

  // cache the start time of the task
  private long startTimeInMillis = -1;


  public void setMaxRetryCount(int maxRetryCount) {
    this.maxRetryCount = maxRetryCount;
  }

  public void setTimeoutIntervalInMillis(long timeoutIntervalInMillis) {
    this.timeoutIntervalInMillis = timeoutIntervalInMillis;
  }

  public void setSleepTime(long sleepTime) {
    this.sleepTime = sleepTime;
  }

  /**
   * Template method handling time/retry constraints.
   * Wraps the "run" method of plain Runnable implementations.
   */
  @Override
  public void run() {
    int retries = 0;
    startTimeInMillis = Calendar.getInstance().getTimeInMillis();
    LOGGER.info("Task started at: {}", startTimeInMillis);

    boolean readyToRun = readyToRun();

    while (withinConstraints(retries) && !readyToRun) {
      retries++;

      // "sleep for a while" and handle interruptions
      sleep();

      // checks for task specific wait condition
      readyToRun = readyToRun();
    }

    if (readyToRun) {
      LOGGER.debug("Task ready for execution! Spent time in millis: {}, Retries: {}", spentMillis(), retries,
          spentMillis());
      //executing the task!
      doRun();
    } else {
      LOGGER.debug("Task exiting due to time/retry constraint excess. Retry count: {}, Timeout: {}", retries,
          spentMillis());
    }
  }

  /**
   * Task specific wait condition to be satisfied prior to run task logic
   *
   * @return true if the wait condition is satisfied, false otherwise
   */
  protected abstract boolean readyToRun();

  /**
   * The logic to be performed by the Runnable.
   *
   * @return
   */
  protected abstract void doRun();

  private boolean withinConstraints(int retries) {
    LOGGER.debug("Checking task run constraints...");
    return !timedOut() && !retriesExceeded(retries);
  }

  private boolean timedOut() {
    LOGGER.debug("Checking time constraint ...");
    boolean timedOut = false;
    if (timeoutIntervalInMillis > 0) {
      timedOut = spentMillis() > timeoutIntervalInMillis;
      LOGGER.debug("Time constraint: [{}]", timedOut ? "EXCEEDED" : "PASSED");
    } else {
      LOGGER.debug("No time constraints defined");
    }
    return timedOut;
  }

  private boolean retriesExceeded(int retries) {
    LOGGER.debug("Checking retry constraint ...");
    boolean exceeded = false;
    if (maxRetryCount > 0) {
      exceeded = retries > maxRetryCount;
      LOGGER.debug("Retry constraint: [{}]", exceeded ? "EXCEEDED" : "PASSED");
    } else {
      LOGGER.debug("No retry constraint defined!");
    }
    return exceeded;
  }

  private long spentMillis() {
    long spentMillis = 0;
    if (startTimeInMillis > 0) {
      spentMillis = Calendar.getInstance().getTimeInMillis() - startTimeInMillis;
    }
    return spentMillis;
  }

  // generic wait logic used to sleep the thread
  private void sleep() {
    try {
      LOGGER.debug("Sleeping for: {} milliseconds ...", sleepTime);
      Thread.sleep(sleepTime);
    } catch (InterruptedException e) {
      // just logging and allowing config flag to be reset
      LOGGER.info("Waiting thread interrupted by exception", e);

      // reset interrupted flag on thread
      Thread.interrupted();
    }
  }
}
