(ns hcknws.prefs
    (:require [cljs.nodejs :as node]))

(def Preferences (node/require "preferences"))
(def prefs (new Preferences "hcknws"))

(defn get-prefs
  ([]
   prefs)
  ([key]
   (aget prefs key)))

(defn set-prefs [k v]
  (aset prefs k v))

(defn clear-prefs [k]
  (js-delete prefs k))
