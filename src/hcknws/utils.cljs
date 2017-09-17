(ns hcknws.utils)

(defn js-> [j]
  (js->clj j :keywordize-keys true))

(defn json-> [j]
  (js-> (.parse js/JSON j)))
