#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""
import os

from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_directory import HdfsDirectory

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/hostLevelParams/stack_name", None)

# This is expected to be of the form #.#.#.#
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

# default hadoop parameters
hadoop_home = '/usr'
hadoop_bin_dir = "/usr/bin"
hadoop_conf_dir = "/etc/hadoop/conf"
tez_etc_dir = "/etc/tez"
config_dir = "/etc/tez/conf"
path_to_tez_examples_jar = "/usr/lib/tez/tez-mapreduce-examples*.jar"

# hadoop parameters for 2.2+
if Script.is_hdp_stack_greater_or_equal("2.2"):
  hadoop_bin_dir = "/usr/hdp/current/hadoop-client/bin"
  hadoop_conf_dir = "/usr/hdp/current/hadoop-client/conf"
  path_to_tez_examples_jar = "/usr/hdp/{hdp_version}/tez/tez-examples*.jar"

# tez only started linking /usr/hdp/x.x.x.x/tez-client/conf in HDP 2.3+
if Script.is_hdp_stack_greater_or_equal("2.3"):
  # !!! use realpath for now since the symlink exists but is broken and a
  # broken symlink messes with the DirectoryProvider class
  config_dir = os.path.realpath("/usr/hdp/current/tez-client/conf")

kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']

java64_home = config['hostLevelParams']['java_home']

tez_user = config['configurations']['tez-env']['tez_user']
user_group = config['configurations']['cluster-env']['user_group']
tez_env_sh_template = config['configurations']['tez-env']['content']

import functools
# Create partial functions with common arguments for every HdfsDirectory call
# to create hdfs directory we need to call params.HdfsDirectory in code
HdfsDirectory = functools.partial(
  HdfsDirectory,
  conf_dir=hadoop_conf_dir,
  hdfs_user=hdfs_user,
  security_enabled=security_enabled,
  keytab=hdfs_user_keytab,
  kinit_path_local=kinit_path_local,
  bin_dir=hadoop_bin_dir
)

