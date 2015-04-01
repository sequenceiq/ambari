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

describe('App.ToggleConfigWidgetView', function () {

  beforeEach(function () {

    this.view = App.ToggleConfigWidgetView.create({
      initSwitcher: Em.K,
      initPopover: Em.K,
      config: Em.Object.create({
        name: 'a.b.c',
        value: 'true',
        defaultValue: 'true',
        stackConfigProperty: Em.Object.create({
          valueAttributes: {
            "type": "value-list",
            "entries": ["true", "false"],
            "entry_labels": ["Active", "Inactive"],
            "entries_editable": "false",
            "selection_cardinality": 1
          }
        })
      })
    });
    this.view.didInsertElement();
  });

  afterEach(function() {
    this.view.destroy();
    this.view = null;
  });

  describe('#getNewSwitcherValue', function () {

    it('should represent string value to boolean', function () {
      expect(this.view.getNewSwitcherValue('false')).to.be.false;
      expect(this.view.getNewSwitcherValue('true')).to.be.true;
    });

  });

  describe('#updateConfigValue', function () {

    it('should represent boolean value to string', function () {
      this.view.set('switcherValue', false);
      expect(this.view.get('config.value')).to.equal('false');
      this.view.set('switcherValue', true);
      expect(this.view.get('config.value')).to.equal('true');
    });

  });

});
