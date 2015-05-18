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

var validator = require('utils/validator');
var stringUtils = require('utils/string_utils');
require('utils/configs/modification_handlers/modification_handler');

App.ServiceConfigsByCategoryView = Em.View.extend(App.UserPref, App.ConfigOverridable, {

  templateName: require('templates/common/configs/service_config_category'),

  classNames: ['accordion-group', 'common-config-category'],

  classNameBindings: ['category.name', 'isShowBlock::hidden'],

  content: null,

  category: null,

  service: null,

  canEdit: true, // View is editable or read-only?

  serviceConfigs: null, // General, Advanced, NameNode, SNameNode, DataNode, etc.

  /**
   * @type {Array}
   */
  categoryConfigs: function () {
    // sort content type configs, sort the rest of configs based on index and then add content array at the end (as intended)
    var categoryConfigs = this.get('categoryConfigsAll'),
      contentOrderedArray = this.orderContentAtLast(categoryConfigs.filterProperty('displayType','content')),
      contentFreeConfigs = categoryConfigs.filter(function(config) {return config.get('displayType')!=='content';}),
      indexOrdered = this.sortByIndex(contentFreeConfigs);

    return indexOrdered.concat(contentOrderedArray).filterProperty('isVisible', true);
  }.property('categoryConfigsAll.@each.isVisible').cacheable(),

  /**
   * This method provides all the properties which apply
   * to this category, irrespective of visibility. This
   * is helpful in Oozie/Hive database configuration, where
   * MySQL etc. database options don't show up, because
   * they were not visible initially.
   */
   categoryConfigsAll: function () {
     return this.get('serviceConfigs').filterProperty('category', this.get('category.name'));
   }.property('serviceConfigs.@each').cacheable(),

  /**
   * If added/removed a serverConfigObject, this property got updated.
   * Without this property, all serviceConfigs Objects will show up even if some was collapsed before.
   * @type {boolean}
   */
  isCategoryBodyVisible: function () {
    return this.get('category.isCollapsed') ? "display: none;" : "display: block;"
  }.property('serviceConfigs.length'),

  /**
   * Should we show config group or not
   * @type {boolean}
   */
  isShowBlock: function () {
    var isCustomPropertiesCategory = this.get('category.customCanAddProperty');
    var emptyFiltered = this.get('categoryConfigs').filterProperty('isHiddenByFilter', false).length > 0;
    var isWidgetsOnlyCategory = this.get('categoryConfigs.length') == this.get('categoryConfigs').filterProperty('widget').length;
    return isCustomPropertiesCategory && this.get('controller.filter') === '' && !this.get('parentView.columns').someProperty('selected') || (emptyFiltered && !isWidgetsOnlyCategory);
  }.property('category.customCanAddProperty', 'categoryConfigs.@each.isHiddenByFilter', 'categoryConfigs.@each.widget', 'controller.filter', 'parentView.columns.@each.selected'),

  /**
   * Re-order the configs to list content displayType properties at last in the category
   * @param categoryConfigs
   */
  orderContentAtLast: function (categoryConfigs) {
    var contentProperties = categoryConfigs.filterProperty('displayType', 'content');
    if (!contentProperties.length) {
      return categoryConfigs
    }
    else {
      return categoryConfigs.sort(function (a, b) {
        var aContent = contentProperties.someProperty('name', a.get('name'));
        var bContent = contentProperties.someProperty('name', b.get('name'));
        if (aContent && bContent) {
          return 0;
        }
        else if (aContent) {
          return 1;
        }
        else {
          return -1;
        }
      });
    }
  },

  /**
   * Warn/prompt user to adjust Service props when changing user/groups in Misc
   * Is triggered when user ended editing text field
   */
  configChangeObserver: function (manuallyChangedProperty) {
    var changedProperty;
    if (manuallyChangedProperty.get("id")) {
      changedProperty = [manuallyChangedProperty];
    }
    else {
      changedProperty = this.get("serviceConfigs").filterProperty("editDone", true);
    }

    if (changedProperty.length > 0) {
      changedProperty = changedProperty.objectAt(0);
    }
    else {
      return;
    }
    this.affectedProperties = [];
    var stepConfigs = this.get("controller.stepConfigs");
    var serviceId = this.get('controller.selectedService.serviceName');
    var serviceConfigModificationHandler = null;
    try{
      serviceConfigModificationHandler = require('utils/configs/modification_handlers/'+serviceId.toLowerCase());
    }catch (e) {
      console.log("Unable to load modification handler for ", serviceId);
    }
    if (serviceConfigModificationHandler != null) {
      var securityEnabled = App.router.get('mainAdminSecurityController.securityEnabled');
      this.affectedProperties = serviceConfigModificationHandler.getDependentConfigChanges(changedProperty, this.get("controller.selectedServiceNames"), stepConfigs, securityEnabled);
    }
    changedProperty.set("editDone", false); // Turn off flag

    if (this.affectedProperties.length > 0 && !this.get("controller.miscModalVisible")) {
      this.newAffectedProperties = this.affectedProperties;
      var self = this;
      return App.ModalPopup.show({
        classNames: ['modal-690px-width'],
        showCloseButton: false,
        header: "Warning: you must also change these Service properties",
        onApply: function () {
          self.get("newAffectedProperties").forEach(function(item) {
            if (item.isNewProperty) {
              self.createProperty({
                name: item.propertyName,
                displayName: item.propertyDisplayName,
                value: item.newValue,
                categoryName: item.categoryName,
                serviceName: item.serviceName,
                filename: item.filename
              });
            } else {
              self.get("controller.stepConfigs").findProperty("serviceName", item.serviceName).get("configs").find(function(config) {
                return item.propertyName == config.get('name') && (item.filename == null || item.filename == config.get('filename'));
              }).set("value", item.newValue);
            }
          });
          self.get("controller").set("miscModalVisible", false);
          this.hide();
        },
        onIgnore: function () {
          self.get("controller").set("miscModalVisible", false);
          this.hide();
        },
        onUndo: function () {
          var affected = self.get("newAffectedProperties").objectAt(0),
            changedProperty = self.get("controller.stepConfigs").findProperty("serviceName", affected.sourceServiceName)
              .get("configs").findProperty("name", affected.changedPropertyName);
          changedProperty.set('value', changedProperty.get('savedValue'));
          self.get("controller").set("miscModalVisible", false);
          this.hide();
        },
        footerClass: Em.View.extend({
          classNames: ['modal-footer'],
          templateName: require('templates/common/configs/propertyDependence_footer'),
          canIgnore: serviceId == 'MISC'
        }),
        bodyClass: Em.View.extend({
          templateName: require('templates/common/configs/propertyDependence'),
          controller: this,
          propertyChange: self.get("newAffectedProperties"),
          didInsertElement: function () {
            self.get("controller").set("miscModalVisible", true);
          }
        })
      });
    }
  }.observes('categoryConfigs.@each.editDone'),

  /**
   * When the view is in read-only mode, it marks
   * the properties as read-only.
   */
  updateReadOnlyFlags: function () {
    var configs = this.get('serviceConfigs');
    var canEdit = this.get('canEdit');
    if (!canEdit && configs) {
      configs.forEach(function (c) {
        c.set('isEditable', false);
        var overrides = c.get('overrides');
        if (overrides != null) {
          overrides.setEach('isEditable', false);
        }
      });
    }
  },

  /**
   * Filtered <code>categoryConfigs</code> array. Used to show filtered result
   */
  filteredCategoryConfigs: function () {
    $('.popover').remove();
    var filter = this.get('parentView.filter').toLowerCase();
    var selectedFilters = this.get('parentView.columns').filterProperty('selected');
    var filteredResult = this.get('categoryConfigs');
    var isInitialRendering = !arguments.length || arguments[1] != 'categoryConfigs';

    if (selectedFilters.length > 0 || filter.length > 0 || this.get('state') === 'inDOM') {
      filteredResult.forEach(function (config) {
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

        var searchString = config.get('savedValue') + config.get('description') +
          config.get('displayName') + config.get('name') + config.get('value') + config.getWithDefault('stackConfigProperty.displayName', '');

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
      });
    }
    filteredResult = this.sortByIndex(filteredResult);
    filteredResult = filteredResult.filterProperty('isHiddenByFilter', false);

    if (filter) {
      if (filteredResult.length && typeof this.get('category.collapsedByDefault') === 'undefined') {
        // Save state
        this.set('category.collapsedByDefault', this.get('category.isCollapsed'));
      }
      this.set('category.isCollapsed', !filteredResult.length);
    } else if (typeof this.get('category.collapsedByDefault') !== 'undefined') {
      // If user clear filter -- restore defaults
      this.set('category.isCollapsed', this.get('category.collapsedByDefault'));
      this.set('category.collapsedByDefault', undefined);
    } else if (isInitialRendering && !filteredResult.length) {
      this.set('category.isCollapsed', true);
    }

    var categoryBlock = $('.' + this.get('category.name').split(' ').join('.') + '>.accordion-body');
    this.get('category.isCollapsed') ? categoryBlock.hide() : categoryBlock.show();
  }.observes('categoryConfigs', 'parentView.filter', 'parentView.columns.@each.selected'),

  /**
   * sort configs in current category by index
   * @param configs
   * @return {Array}
   * @method sortByIndex
   */
  sortByIndex: function (configs) {
    var sortedConfigs = [];
    var unSorted = [];
    if (!configs.someProperty('index')) {
      return configs;
    }
    configs.forEach(function (config) {
      var index = config.get('index');
      if ((index !== null) && isFinite(index)) {
        sortedConfigs[index] ? sortedConfigs.splice(index, 0, config) : sortedConfigs[index] = config;
      } else {
        unSorted.push(config);
      }
    });
    // remove undefined elements from array
    sortedConfigs = sortedConfigs.filter(function (config) {
      return config !== undefined;
    });
    return sortedConfigs.concat(unSorted);
  },

  /**
   * Onclick handler for Config Group Header. Used to show/hide block
   * @method onToggleBlock
   */
  onToggleBlock: function () {
    this.$('.accordion-body').toggle('blind', 500);
    this.set('category.isCollapsed', !this.get('category.isCollapsed'));
  },

  didInsertElement: function () {
    var isCollapsed = this.get('category.isCollapsed') == undefined ? (this.get('category.name').indexOf('Advanced') != -1 || this.get('category.name').indexOf('CapacityScheduler') != -1 || this.get('category.name').indexOf('Custom') != -1) : this.get('category.isCollapsed');
    var self = this;
    this.set('category.isCollapsed', isCollapsed);
    if (isCollapsed) {
      this.$('.accordion-body').hide();
    }
    else {
      this.$('.accordion-body').show();
    }
    $('#serviceConfig').tooltip({
      selector: '[data-toggle=tooltip]',
      placement: 'top'
    });
    this.updateReadOnlyFlags();
    this.filteredCategoryConfigs();
    Em.run.next(function () {
      self.updateReadOnlyFlags();
    });
  },

  willDestroyElement: function () {
    if (this.get('parentView.controller.name') == 'mainServiceInfoConfigsController') {
      this.get('categoryConfigsAll').forEach(function (item) {
        item.set('isVisible', false);
      });
    }
  },

  isOneOfAdvancedSections: function () {
    var category = this.get('category');
    return category.indexOf("Advanced") != -1;
  },

  /**
   * @returns {string}
   */
  persistKey: function () {
    return 'admin-bulk-add-properties-' + App.router.get('loginName');
  },

  isSecureConfig: function (configName, filename) {
    var secureConfigs = this.get('controller.secureConfigs').filterProperty('filename', filename);
    return !!secureConfigs.findProperty('name', configName);
  },

  createProperty: function (propertyObj) {
    var selectedConfigGroup = this.get('controller.selectedConfigGroup'),
      isSecureConfig = this.isSecureConfig(propertyObj.name, propertyObj.filename);
    this.get('serviceConfigs').pushObject(App.ServiceConfigProperty.create({
      name: propertyObj.name,
      displayName: propertyObj.displayName || propertyObj.name,
      value: propertyObj.value,
      displayType: stringUtils.isSingleLine(propertyObj.value) ? 'advanced' : 'multiLine',
      isSecureConfig: isSecureConfig,
      category: propertyObj.categoryName,
      id: 'site property',
      serviceName: propertyObj.serviceName,
      savedValue: null,
      recommendedValue: null,
      supportsFinal: App.config.shouldSupportFinal(propertyObj.serviceName, propertyObj.filename),
      filename: propertyObj.filename || '',
      isUserProperty: true,
      isNotSaved: true,
      isRequired: false,
      group: selectedConfigGroup.get('isDefault') ? null : selectedConfigGroup,
      isOverridable: selectedConfigGroup.get('isDefault')
    }));
  },

  /**
   * Show popup for adding new config-properties
   * @method showAddPropertyWindow
   */
  showAddPropertyWindow: function () {
    var persistController = this;
    var modePersistKey = this.persistKey();
    var selectedConfigGroup = this.get('controller.selectedConfigGroup');

    persistController.getUserPref(modePersistKey).pipe(function (data) {
      return !!data;
    },function () {
      return false;
    }).always((function (isBulkMode) {

        var category = this.get('category');
        var siteFileName = category.get('siteFileName');

        var service = this.get('service');
        var serviceName = service.get('serviceName');

        var configsOfFile = service.get('configs').filterProperty('filename', siteFileName);
        var siteFileProperties = App.config.get('configMapping').all().filterProperty('filename', siteFileName);

        function isDuplicatedConfigKey(name) {
          return siteFileProperties.findProperty('name', name) || configsOfFile.findProperty('name', name);
        }

        var serviceConfigObj = Ember.Object.create({
          isBulkMode: isBulkMode,
          bulkConfigValue: '',
          bulkConfigError: false,
          bulkConfigErrorMessage: '',

          name: '',
          value: '',
          isKeyError: false,
          showFilterLink: false,
          errorMessage: '',
          observeAddPropertyValue: function () {
            var name = this.get('name');
            if (name.trim() != '') {
              if (validator.isValidConfigKey(name)) {
                if (!isDuplicatedConfigKey(name)) {
                  this.set('showFilterLink', false);
                  this.set('isKeyError', false);
                  this.set('errorMessage', '');
                } else {
                  this.set('showFilterLink', true);
                  this.set('isKeyError', true);
                  this.set('errorMessage', Em.I18n.t('services.service.config.addPropertyWindow.error.derivedKey'));
                }
              } else {
                this.set('showFilterLink', false);
                this.set('isKeyError', true);
                this.set('errorMessage', Em.I18n.t('form.validator.configKey'));
              }
            } else {
              this.set('showFilterLink', false);
              this.set('isKeyError', true);
              this.set('errorMessage', Em.I18n.t('services.service.config.addPropertyWindow.error.required'));
            }
          }.observes('name')
        });

        function processConfig(config, callback) {
          var lines = config.split('\n');
          var errorMessages = [];
          var parsedConfig = {};
          var propertyCount = 0;

          function lineNumber(index) {
            return Em.I18n.t('services.service.config.addPropertyWindow.error.lineNumber').format(index + 1);
          }

          lines.forEach(function (line, index) {
            if (line.trim() === '') {
              return;
            }
            var delimiter = '=';
            var delimiterPosition = line.indexOf(delimiter);
            if (delimiterPosition === -1) {
              errorMessages.push(lineNumber(index) + Em.I18n.t('services.service.config.addPropertyWindow.error.format'));
              return;
            }
            var key = Em.Handlebars.Utils.escapeExpression(line.slice(0, delimiterPosition).trim());
            var value = line.slice(delimiterPosition + 1);
            if (validator.isValidConfigKey(key)) {
              if (!isDuplicatedConfigKey(key) && !(key in parsedConfig)) {
                parsedConfig[key] = value;
                propertyCount++;
              } else {
                errorMessages.push(lineNumber(index) + Em.I18n.t('services.service.config.addPropertyWindow.error.derivedKey.specific').format(key));
              }
            } else {
              errorMessages.push(lineNumber(index) + Em.I18n.t('form.validator.configKey.specific').format(key));
            }
          });

          if (errorMessages.length > 0) {
            callback(errorMessages.join('<br>'), parsedConfig);
          }
          else if (propertyCount === 0) {
            callback(Em.I18n.t('services.service.config.addPropertyWindow.propertiesPlaceholder', parsedConfig));
          }
          else {
            callback(null, parsedConfig);
          }
        }

        App.ModalPopup.show({
          classNames: ['sixty-percent-width-modal'],
          header: 'Add Property',
          primary: 'Add',
          secondary: 'Cancel',
          onPrimary: function () {
            var propertyObj = {
              filename: siteFileName,
              serviceName: serviceName,
              categoryName: category.get('name')
            };
            if (serviceConfigObj.isBulkMode) {
              var popup = this;
              processConfig(serviceConfigObj.bulkConfigValue, function (error, parsedConfig) {
                if (error) {
                  serviceConfigObj.set('bulkConfigError', true);
                  serviceConfigObj.set('bulkConfigErrorMessage', error);
                } else {
                  for (var key in parsedConfig) {
                    if (parsedConfig.hasOwnProperty(key)) {
                      propertyObj.name = key;
                      propertyObj.value = parsedConfig[key];
                      persistController.createProperty(propertyObj);
                    }
                  }
                  popup.hide();
                }
              });
            }
            else {
              serviceConfigObj.observeAddPropertyValue();
              /**
               * For the first entrance use this if (serviceConfigObj.name.trim() != '')
               */
              if (!serviceConfigObj.isKeyError) {
                propertyObj.name = serviceConfigObj.get('name');
                propertyObj.value = serviceConfigObj.get('value');
                persistController.createProperty(propertyObj);
                this.hide();
              }
            }
          },
          bodyClass: Em.View.extend({
            fileName: siteFileName,
            notMisc: function () {
              return serviceName !== 'MISC';
            }.property(),
            templateName: require('templates/common/configs/addPropertyWindow'),
            controllerBinding: 'App.router.mainServiceInfoConfigsController',
            serviceConfigObj: serviceConfigObj,
            didInsertElement: function () {
              App.tooltip(this.$("[data-toggle=tooltip]"), {
                placement: "top"
              });
            },
            toggleBulkMode: function () {
              this.toggleProperty('serviceConfigObj.isBulkMode');
              persistController.postUserPref(modePersistKey, this.get('serviceConfigObj.isBulkMode'));
            },
            filterByKey: function (event) {
              var controller = (App.router.get('currentState.name') == 'configs')
                ? App.router.get('mainServiceInfoConfigsController')
                : App.router.get('wizardStep7Controller');
              this.get('parentView').onClose();
              controller.set('filter', event.view.get('serviceConfigObj.name'));
            }
          })
        });

      }).bind(this));
  },

  /**
   * Toggle <code>isFinal</code> for selected config-property if <code>isNotEditable</code> is false
   * @param {object} event
   * @method toggleFinalFlag
   */
  toggleFinalFlag: function (event) {
    var serviceConfigProperty = event.contexts[0];
    if (serviceConfigProperty.get('isNotEditable')) {
      return;
    }
    serviceConfigProperty.toggleProperty('isFinal');
  },

  /**
   * Removes the top-level property from list of properties.
   * Should be only called on user properties.
   * @method removeProperty
   */
  removeProperty: function (event) {
    var serviceConfigProperty = event.contexts[0];
    this.get('serviceConfigs').removeObject(serviceConfigProperty);
    if (App.get('isClusterSupportsEnhancedConfigs')) {
      var deletedConfig = App.ConfigProperty.find().find(function(cp) {
        return cp.get('name') === serviceConfigProperty.get('name')
          && cp.get('fileName') === serviceConfigProperty.get('filename')
          && cp.get('isOriginalSCP');
      });
      if (deletedConfig) {
        deletedConfig.deleteRecord();
        App.store.commit();
      }
    }
    // push config's file name if this config was stored on server
    if (!serviceConfigProperty.get('isNotSaved')) {
      this.get('controller').get('modifiedFileNames').push(serviceConfigProperty.get('filename'));
    }
    Em.$('body>.tooltip').remove(); //some tooltips get frozen when their owner's DOM element is removed
  },

  /**
   * Set config's value to recommended
   * @param event
   * @method setRecommendedValue
   */
  setRecommendedValue: function (event) {
    var serviceConfigProperty = event.contexts[0];
    serviceConfigProperty.set('value', serviceConfigProperty.get('recommendedValue'));
  },

  /**
   * Restores given property's value to be its default value.
   * Does not update if there is no default value.
   * @method doRestoreDefaultValue
   */
  doRestoreDefaultValue: function (event) {
    var serviceConfigProperty = event.contexts[0];
    var value = serviceConfigProperty.get('value');
    var savedValue = serviceConfigProperty.get('savedValue');
    var supportsFinal = serviceConfigProperty.get('supportsFinal');
    var savedIsFinal = serviceConfigProperty.get('savedIsFinal');

    if (savedValue != null) {
      if (serviceConfigProperty.get('displayType') === 'password') {
        serviceConfigProperty.set('retypedPassword', savedValue);
      }
      serviceConfigProperty.set('value', savedValue);
    }
    if (supportsFinal) {
      serviceConfigProperty.set('isFinal', savedIsFinal);
    }
    this.configChangeObserver(serviceConfigProperty);
    Em.$('body>.tooltip').remove(); //some tooltips get frozen when their owner's DOM element is removed
  }

});
