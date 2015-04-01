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


// load all mixins here

require('mixins/common/blueprint');
require('mixins/common/localStorage');
require('mixins/common/userPref');
require('mixins/common/reload_popup');
require('mixins/common/serverValidator');
require('mixins/common/table_server_view_mixin');
require('mixins/common/table_server_mixin');
require('mixins/main/host/details/host_components/decommissionable');
//require('mixins/main/service/themes_support');
require('mixins/main/service/configs/config_overridable');
require('mixins/main/service/configs/preload_requests_chain');
require('mixins/main/service/configs/widget_popover_support');
require('mixins/routers/redirections');
require('mixins/wizard/wizardProgressPageController');
require('mixins/wizard/wizardDeployProgressController');
require('mixins/wizard/wizardProgressPageView');
require('mixins/wizard/wizardDeployProgressView');
require('mixins/wizard/wizardEnableDone');
require('mixins/wizard/selectHost');
require('mixins/wizard/addSecurityConfigs');
require('mixins/wizard/wizard_menu_view');
require('mixins/common/configs/enhanced_configs');
require('mixins/common/widget_mixin');
