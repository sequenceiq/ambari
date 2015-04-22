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

var App = require('app');

App.NumberWidgetView = Em.View.extend(App.WidgetMixin, {
  templateName: require('templates/common/widget/number_widget'),

  /**
   * @type {string}
   */
  value: '',

  displayValue: function () {
    var value = parseFloat(this.get('value'));
    if (isNaN(value)) return Em.I18n.t('common.na');
    var value = value % 1 != 0? value.toFixed(2): value;
    var unit = this.get('content.properties.display_unit')? this.get('content.properties.display_unit'): '';
    return value + unit;
  }.property('value'),

  /**
   * common metrics container
   * @type {Array}
   */
  metrics: [],

  /**
   * color of content calculated by thresholds
   * @type {string}
   */
  contentColor: function () {
    var value = parseFloat(this.get('value'));
    var warningThreshold = parseFloat(this.get('content.properties.warning_threshold'));
    var errorThreshold = parseFloat(this.get('content.properties.error_threshold'));

    if (isNaN(value)) {
      return 'grey';
    } else if (isNaN(warningThreshold) || isNaN(errorThreshold) || value <= warningThreshold) {
      return 'green';
    } else if (value <= errorThreshold) {
      return 'orange';
    } else {
      return 'red';
    }
  }.property('value', 'content.properties.warning_threshold', 'content.properties.error_threshold')
});