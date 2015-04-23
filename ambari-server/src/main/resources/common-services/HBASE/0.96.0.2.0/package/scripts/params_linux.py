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

from ambari_commons.constants import AMBARI_SUDO_BINARY
from functions import calc_xmn_from_xms
from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from resource_management import *
import status_params
import json

# server configurations
config = Script.get_config()
exec_tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = default("/hostLevelParams/stack_name", None)

version = default("/commandParams/version", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

#hadoop params
if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
  hadoop_bin_dir = format("/usr/hdp/current/hadoop-client/bin")
  daemon_script = format('/usr/hdp/current/hbase-client/bin/hbase-daemon.sh')
  region_mover = format('/usr/hdp/current/hbase-client/bin/region_mover.rb')
  region_drainer = format('/usr/hdp/current/hbase-client/bin/draining_servers.rb')
  hbase_cmd = format('/usr/hdp/current/hbase-client/bin/hbase')
else:
  hadoop_bin_dir = "/usr/bin"
  daemon_script = "/usr/lib/hbase/bin/hbase-daemon.sh"
  region_mover = "/usr/lib/hbase/bin/region_mover.rb"
  region_drainer = "/usr/lib/hbase/bin/draining_servers.rb"
  hbase_cmd = "/usr/lib/hbase/bin/hbase"

# no symlink for phoenix-server at this point
phx_daemon_script = '/usr/hdp/current/phoenix-server/bin/queryserver.py'

hadoop_conf_dir = "/etc/hadoop/conf"
hbase_conf_dir_prefix = "/etc/hbase"
hbase_conf_dir = format("{hbase_conf_dir_prefix}/conf")
hbase_excluded_hosts = config['commandParams']['excluded_hosts']
hbase_drain_only = default("/commandParams/mark_draining_only",False)
hbase_included_hosts = config['commandParams']['included_hosts']

hbase_user = status_params.hbase_user
hbase_principal_name = config['configurations']['hbase-env']['hbase_principal_name']
smokeuser = config['configurations']['cluster-env']['smokeuser']
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = config['configurations']['cluster-env']['security_enabled']

# this is "hadoop-metrics.properties" for 1.x stacks
metric_prop_file_name = "hadoop-metrics2-hbase.properties"

# not supporting 32 bit jdk.
java64_home = config['hostLevelParams']['java_home']

log_dir = config['configurations']['hbase-env']['hbase_log_dir']
master_heapsize = config['configurations']['hbase-env']['hbase_master_heapsize']

regionserver_heapsize = config['configurations']['hbase-env']['hbase_regionserver_heapsize']
regionserver_xmn_max = config['configurations']['hbase-env']['hbase_regionserver_xmn_max']
regionserver_xmn_percent = config['configurations']['hbase-env']['hbase_regionserver_xmn_ratio']
regionserver_xmn_size = calc_xmn_from_xms(regionserver_heapsize, regionserver_xmn_percent, regionserver_xmn_max)

if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
  hbase_max_direct_memory_size  = config['configurations']['hbase-env']['hbase_max_direct_memory_size']

pid_dir = status_params.pid_dir
tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']
# TODO UPGRADE default, update site during upgrade
_local_dir_conf = default('/configurations/hbase-site/hbase.local.dir', "${hbase.tmp.dir}/local")
local_dir = substitute_vars(_local_dir_conf, config['configurations']['hbase-site'])

client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
master_jaas_config_file = format("{hbase_conf_dir}/hbase_master_jaas.conf")
regionserver_jaas_config_file = format("{hbase_conf_dir}/hbase_regionserver_jaas.conf")

ganglia_server_hosts = default('/clusterHostInfo/ganglia_server_host', []) # is not passed when ganglia is not present
ganglia_server_host = '' if len(ganglia_server_hosts) == 0 else ganglia_server_hosts[0]

ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
has_metric_collector = not len(ams_collector_hosts) == 0
if has_metric_collector:
  metric_collector_host = ams_collector_hosts[0]
  metric_collector_port = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
  if metric_collector_port and metric_collector_port.find(':') != -1:
    metric_collector_port = metric_collector_port.split(':')[1]
  pass

# if hbase is selected the hbase_rs_hosts, should not be empty, but still default just in case
if 'slave_hosts' in config['clusterHostInfo']:
  rs_hosts = default('/clusterHostInfo/hbase_rs_hosts', '/clusterHostInfo/slave_hosts') #if hbase_rs_hosts not given it is assumed that region servers on same nodes as slaves
else:
  rs_hosts = default('/clusterHostInfo/hbase_rs_hosts', '/clusterHostInfo/all_hosts') 

smoke_test_user = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']
smokeuser_permissions = "RWXCA"
service_check_data = functions.get_unique_id_and_date()
user_group = config['configurations']['cluster-env']["user_group"]

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  master_jaas_princ = config['configurations']['hbase-site']['hbase.master.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  regionserver_jaas_princ = config['configurations']['hbase-site']['hbase.regionserver.kerberos.principal'].replace('_HOST',_hostname_lowercase)

master_keytab_path = config['configurations']['hbase-site']['hbase.master.keytab.file']
regionserver_keytab_path = config['configurations']['hbase-site']['hbase.regionserver.keytab.file']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
hbase_user_keytab = config['configurations']['hbase-env']['hbase_user_keytab']
kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
if security_enabled:
  kinit_cmd = format("{kinit_path_local} -kt {hbase_user_keytab} {hbase_principal_name};")
else:
  kinit_cmd = ""

#log4j.properties
if (('hbase-log4j' in config['configurations']) and ('content' in config['configurations']['hbase-log4j'])):
  log4j_props = config['configurations']['hbase-log4j']['content']
else:
  log4j_props = None
  
hbase_env_sh_template = config['configurations']['hbase-env']['content']

hbase_hdfs_root_dir = config['configurations']['hbase-site']['hbase.rootdir']
hbase_staging_dir = "/apps/hbase/staging"
#for create_hdfs_directory
hostname = config["hostname"]
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
import functools
#create partial functions with common arguments for every HdfsDirectory call
#to create hdfs directory we need to call params.HdfsDirectory in code
HdfsDirectory = functools.partial(
  HdfsDirectory,
  conf_dir=hadoop_conf_dir,
  hdfs_user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  bin_dir = hadoop_bin_dir
)

if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
  command_role = default("/role", "")
  if command_role == "HBASE_MASTER" or command_role == "HBASE_REGIONSERVER":
    role_root = "master" if command_role == "HBASE_MASTER" else "regionserver"

    daemon_script=format("/usr/hdp/current/hbase-{role_root}/bin/hbase-daemon.sh")
    region_mover = format("/usr/hdp/current/hbase-{role_root}/bin/region_mover.rb")
    region_drainer = format("/usr/hdp/current/hbase-{role_root}/bin/draining_servers.rb")
    hbase_cmd = format("/usr/hdp/current/hbase-{role_root}/bin/hbase")

if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
  # Setting Flag value for ranger hbase plugin
  enable_ranger_hbase = False
  ranger_plugin_enable = config['configurations']['ranger-hbase-plugin-properties']['ranger-hbase-plugin-enabled']
  if ranger_plugin_enable.lower() == 'yes':
    enable_ranger_hbase = True
  elif ranger_plugin_enable.lower() == 'no':
    enable_ranger_hbase = False

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0    

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

# ranger hbase properties
policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
sql_connector_jar = config['configurations']['admin-properties']['SQL_CONNECTOR_JAR']
xa_audit_db_flavor = config['configurations']['admin-properties']['DB_FLAVOR']
xa_audit_db_name = config['configurations']['admin-properties']['audit_db_name']
xa_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
xa_audit_db_password = config['configurations']['admin-properties']['audit_db_password']
xa_db_host = config['configurations']['admin-properties']['db_host']
repo_name = str(config['clusterName']) + '_hbase'

common_name_for_certificate = config['configurations']['ranger-hbase-plugin-properties']['common.name.for.certificate']

zookeeper_znode_parent = config['configurations']['hbase-site']['zookeeper.znode.parent']
hbase_zookeeper_quorum = config['configurations']['hbase-site']['hbase.zookeeper.quorum']
hbase_zookeeper_property_clientPort = config['configurations']['hbase-site']['hbase.zookeeper.property.clientPort']
hbase_security_authentication = config['configurations']['hbase-site']['hbase.security.authentication']
hadoop_security_authentication = config['configurations']['core-site']['hadoop.security.authentication']

repo_config_username = default("/configurations/ranger-hbase-plugin-properties/REPOSITORY_CONFIG_USERNAME", "hbase")
repo_config_password = default("/configurations/ranger-hbase-plugin-properties/REPOSITORY_CONFIG_PASSWORD", "hbase")

admin_uname = config['configurations']['ranger-env']['admin_username']
admin_password = config['configurations']['ranger-env']['admin_password']

ambari_ranger_admin = config['configurations']['ranger-env']['ranger_admin_username']
ambari_ranger_password = config['configurations']['ranger-env']['ranger_admin_password']
policy_user = config['configurations']['ranger-hbase-plugin-properties']['policy_user']

#For curl command in ranger plugin to get db connector
jdk_location = config['hostLevelParams']['jdk_location']
java_share_dir = '/usr/share/java'
if has_ranger_admin:
  if xa_audit_db_flavor.lower() == 'mysql':
    jdbc_symlink_name = "mysql-jdbc-driver.jar"
    jdbc_jar_name = "mysql-connector-java.jar"
  elif xa_audit_db_flavor.lower() == 'oracle':
    jdbc_jar_name = "ojdbc6.jar"
    jdbc_symlink_name = "oracle-jdbc-driver.jar"
  elif nxa_audit_db_flavor.lower() == 'postgres':
    jdbc_jar_name = "postgresql.jar"
    jdbc_symlink_name = "postgres-jdbc-driver.jar"
  elif xa_audit_db_flavor.lower() == 'sqlserver':
    jdbc_jar_name = "sqljdbc4.jar"
    jdbc_symlink_name = "mssql-jdbc-driver.jar"

  downloaded_custom_connector = format("{exec_tmp_dir}/{jdbc_jar_name}")
  
  driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
  driver_curl_target = format("{java_share_dir}/{jdbc_jar_name}")

hbase_ranger_plugin_config = {
  'username': repo_config_username,
  'password': repo_config_password,
  'hadoop.security.authentication': hadoop_security_authentication,
  'hbase.security.authentication': hbase_security_authentication,
  'hbase.zookeeper.property.clientPort': hbase_zookeeper_property_clientPort,
  'hbase.zookeeper.quorum': hbase_zookeeper_quorum,
  'zookeeper.znode.parent': zookeeper_znode_parent,
  'commonNameForCertificate': common_name_for_certificate,
  'hbase.master.kerberos.principal': master_jaas_princ if security_enabled else ''
}

hbase_ranger_plugin_repo = {
  'isActive': 'true',
  'config': json.dumps(hbase_ranger_plugin_config),
  'description': 'hbase repo',
  'name': repo_name,
  'repositoryType': 'hbase',
  'assetType': '2'
}



