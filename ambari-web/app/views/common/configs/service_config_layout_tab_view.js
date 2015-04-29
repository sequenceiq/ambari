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

App.ServiceConfigLayoutTabView = Em.View.extend(App.ConfigOverridable, {

  /**
   * Determines if view is editable
   * It true - show all control-elements (undo, override, finalize etc) for each widget
   * If false - no widgets control-elements will be shown
   * Bound from template
   * @type {boolean}
   */
  canEdit: true,

  /**
   * @type {App.Service}
   */
  service: function () {
    return this.get('controller.selectedService');
  }.property('controller.selectedService'),

  templateName: require('templates/common/configs/service_config_layout_tab'),

  classNames: ['enhanced-config-tab-content'],
  /**
   * ConfigType-Widget map
   * key - widget type
   * value - widget view
   * @type {object}
   */
  widgetTypeMap: {
    checkbox: App.CheckboxConfigWidgetView,
    combo: App.ComboConfigWidgetView,
    directory: App.DirectoryConfigWidgetView,
    directories: App.DirectoryConfigWidgetView,
    list: App.ListConfigWidgetView,
    password: App.PasswordConfigWidgetView,
    'radio-buttons': App.RadioButtonConfigWidgetView,
    slider: App.SliderConfigWidgetView,
    'text-field': App.TextFieldConfigWidgetView,
    'time-interval-spinner': App.TimeIntervalSpinnerView,
    toggle: App.ToggleConfigWidgetView,
    'text-area': App.StringConfigWidgetView
  },

  /**
   * Prepare configs for render
   * <code>subsection.configs</code> is an array of App.StackConfigProperty, but not App.ConfigProperty,
   * so proper config-properties should be linked to the subsections.
   * Also correct widget should be used for each config (it's selected according to <code>widget.type</code> and
   * <code>widgetTypeMap</code>). It may throw an error if needed widget can't be found in the <code>widgetTypeMap</code>
   * @method prepareConfigProperties
   */
  prepareConfigProperties: function () {
    var widgetTypeMap = this.get('widgetTypeMap');
    var self = this;
    var serviceName = self.get('controller.selectedService.serviceName');
    this.get('content.sectionRows').forEach(function (row) {
      row.forEach(function (section) {
        section.get('subsectionRows').forEach(function (subRow) {
          subRow.forEach(function (subsection) {
            subsection.set('configs', []);
            subsection.get('configProperties').forEach(function (config) {

              var service = self.get('controller.stepConfigs').findProperty('serviceName', serviceName);
              if (!service) return;
              var configProperty = service.get('configs').findProperty('name', config.get('name'));
              if (!configProperty) return;

              subsection.get('configs').pushObject(configProperty);
              var configWidgetType = config.get('widget.type');
              var widget = widgetTypeMap[configWidgetType];
              Em.assert('Unknown config widget view for config ' + configProperty.get('id') + ' with type ' + configWidgetType, widget);
              configProperty.setProperties({
                widget: widget,
                stackConfigProperty: config
              });
              if (configProperty.get('overrides')) {
                configProperty.get('overrides').setEach('stackConfigProperty', config);
              }
              if (configProperty.get('compareConfigs')) {
                configProperty.get('compareConfigs').invoke('setProperties', {
                  isComparison: false,
                  stackConfigProperty: config
                });
              }
            });
          });
        });
      });
    });
  },

  /**
   * Mark isHiddenByFilter flag for configs, sub-sections, and tab
   * @method filterEnhancedConfigs
   */
  filterEnhancedConfigs: function () {
    var self = this;
    this.get('content.sectionRows').forEach(function (row) {
      row.forEach(function (section) {
        section.get('subsectionRows').forEach(function (subRow) {
          subRow.forEach(function (subsection) {
            subsection.get('configs').forEach(function (config) {
              $('.popover').remove();
              var filter = self.get('parentView.filter');
              var selectedFilters = self.get('parentView.columns').filterProperty('selected');

              if (selectedFilters.length > 0 || filter.length > 0 || self.get('state') === 'inDOM') {
                // process selected filters
                var passesFilters = true;
                selectedFilters.forEach(function (filter) {
                  if (config.get(filter.attributeName) !== filter.attributeValue) {
                    passesFilters = false;
                  }
                });

                if (!passesFilters) {
                  config.set('isHiddenByFilter', true);
                  return false;
                }

                // process input filter
                var searchString = config.get('defaultValue') + config.get('description') +
                config.get('displayName') + config.get('name') + config.get('value');
                if (config.get('overrides')) {
                  config.get('overrides').forEach(function (overriddenConf) {
                    searchString += overriddenConf.get('value') + overriddenConf.get('group.name');
                  });
                }

                if (filter != null && typeof searchString === "string") {
                  config.set('isHiddenByFilter', !(searchString.toLowerCase().indexOf(filter) > -1));
                } else {
                  config.set('isHiddenByFilter', false);
                }
              }
            });
            // check subsection
            var allConfigs = subsection.get('configs');
            var allHiddenConfigs = allConfigs.filterProperty('isHiddenByFilter');
            var configsLength = allConfigs.get('length');
            if (configsLength > 0 && configsLength == allHiddenConfigs.length) {
              subsection.set('isHiddenByFilter', true);
            } else {
              subsection.set('isHiddenByFilter', false);
            }
          });
        });
        // check section
        var allSubSections = section.get('subSections');
        var allHiddenSubSections = allSubSections.filterProperty('isHiddenByFilter');
        var subSectionsLength = allSubSections.get('length');
        if (subSectionsLength > 0 && subSectionsLength == allHiddenSubSections.length) {
          section.set('isHiddenByFilter', true);
        } else {
          section.set('isHiddenByFilter', false);
        }
      });
    });
    // process show/hide state for tab
    var tab = this.get('content');
    if (!tab.get('isAdvanced')) {
      var allSections = tab.get('sections');
      var allHiddenSections = allSections.filterProperty('isHiddenByFilter');
      var sectionsLength = allSections.get('length');
      // if all sections in tab are hidden by filter, do not show the tab
      if (sectionsLength > 0 && sectionsLength == allHiddenSections.length) {
        tab.set('isHiddenByFilter', true);
        tab.set('isActive', false);
      } else {
        tab.set('isHiddenByFilter', false);
      }
    }
    // no more active tab? pick the first non hidden tab and make it active
    this.get('parentView').pickActiveTab(this.get('parentView.tabs'));
  }.observes('parentView.filter', 'parentView.columns.@each.selected'),

  didInsertElement: function () {
    this._super();
    this.prepareConfigProperties();
    if (this.get('controller.isCompareMode')) {
      this.filterEnhancedConfigs();
    }
  }

});
