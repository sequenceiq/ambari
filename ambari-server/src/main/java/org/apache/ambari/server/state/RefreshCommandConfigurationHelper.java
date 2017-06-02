package org.apache.ambari.server.state;

import java.util.HashMap;
import java.util.Map;


public class RefreshCommandConfigurationHelper {

  //TODO this will be populated from stack definition
  static Map<String, Map<String, String>> propertyComponentCommandMap = new HashMap(){{
    put("core-site/hadoop.proxyuser", new HashMap() {{
      put("NAMENODE", "reloadproxyusers");}});

    put("hdfs-site/dfs.heartbeat.interval", new HashMap() {{
      put("NAMENODE", RELOAD_CONFIGS);}});
    put("hdfs-site/hadoop.caller.context.enabled", new HashMap() {{
      put("NAMENODE", RELOAD_CONFIGS);}});

    put("hdfs-site/dfs.datanode.data.dir", new HashMap() {{
      put("DATANODE", RELOAD_CONFIGS);}});
  }};

  public static String RELOAD_CONFIGS = "reload_configs";
  public static String REFRESH_CONFIGS = "refresh_configs";

  private static String findKey(String propertyName) {
    for (String keyName : propertyComponentCommandMap.keySet()) {
      if (propertyName.startsWith(keyName)) {
        return keyName;
      }
    }
    return null;
  }

  public static String getRefreshCommandForComponent(String componentName, String propertyName) {
    String keyName = findKey(propertyName);
    Map<String, String> componentCommandMap = propertyComponentCommandMap.get(keyName);
    if (componentCommandMap == null) {
      return null;
    }
    String commandForComponent = componentCommandMap.get(componentName);
    if (commandForComponent != null) {
      return commandForComponent;
    } else if(componentCommandMap.size() > 0) {
      return REFRESH_CONFIGS;
    }
    return null;
  }

}
