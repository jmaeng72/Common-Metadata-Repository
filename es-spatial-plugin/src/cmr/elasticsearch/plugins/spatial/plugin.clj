(ns cmr.elasticsearch.plugins.spatial.plugin
  (:import
   (org.elasticsearch.script ScriptModule)
   (org.elasticsearch.plugins ScriptPlugin)
   (org.elasticsearch.common.settings Settings)
   (org.elasticsearch.plugins Plugin)
   (cmr.elasticsearch.plugins SpatialScriptEngine)
   (java.util Collection Collections))
  (:gen-class
   :name cmr.elasticsearch.plugins.SpatialSearchPlugin
   :extends org.elasticsearch.plugins.Plugin
   :implements [org.elasticsearch.plugins.ScriptPlugin]))

(defn -getScriptEngine
  "Spatial script engine."
  [_this ^Settings _settings ^Collection _contexts]
  (new SpatialScriptEngine))

(defn -getContexts
  "Return script contexts."
  [_this]
  (Collections/emptyList))
