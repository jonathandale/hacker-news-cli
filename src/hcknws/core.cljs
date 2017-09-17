(ns hcknws.core
  (:require [cljs.nodejs :as node]
            [cljs.pprint :refer [pprint]]
            [hcknws.ui :as ui]
            [hcknws.utils :refer [json-> js->]]))

(node/enable-util-print!)

(def rp (node/require "request-promise"))
(def promise (node/require "bluebird"))
(def readline (node/require "readline"))
(def stdout (.-stdout js/process))
(def stdin (.-stdin js/process))
(def rl (.createInterface readline stdin stdout))
(def charm ((node/require "charm") stdout))
(def spawn (.-spawn (node/require "child_process")))
(def chalk (node/require "chalk"))
(def page-count 6)
(def story-height 3)
(def state (atom {:idx 0
                  :page 0
                  :story-ids nil
                  :stories nil
                  :fetching true}))

(defn get-story-ids []
  (let [start (* (:page @state) page-count)]
    (subvec (:story-ids @state) start (min (count (:story-ids @state)) (+ start page-count)))))

(defn get-stories []
  (swap! state assoc :fetching true)
  (-> promise
      (.all (map #(rp (str "https://hacker-news.firebaseio.com/v0/item/" % ".json"))
              (get-story-ids)))
      (.then #(do
                (swap! state assoc :fetching false)
                (map json-> (js-> %))))))

(defn open-story []
  (spawn "open" (clj->js [(:url (nth (:stories @state) (:idx @state))) "--background"])))

(defn clear-stories []
  (.erase charm "line")
  (dotimes [_ (+ (* story-height (count (:stories @state)))
                 1)]
    (do
      (.up charm 1)
      (.erase charm "line"))))

(defn render-meta []
  (str "Page " (:page @state) " of " (.round js/Math (/ (count (:story-ids @state)) page-count))
       (ui/nl)))

(defn render-stories []
  (.write charm (render-meta))
  (doall
    (map-indexed
      (fn [idx {:keys [title] :as story}]
        (.write charm (str (ui/print-story story (= idx (:idx @state))))))
      (:stories @state))))

(defn get-page-stories []
  (-> (get-stories)
      (.then
        (fn [stories]
          (swap! state assoc :stories stories)
          (render-stories)))))

(defn handle-events [_ key]
  (when-not (:fetching @state)
    (let [k (.-name key)]
      (cond
        (= "up" k) (when (pos? (:idx @state))
                      (clear-stories)
                      (swap! state update :idx dec)
                      (render-stories))
        (= "down" k) (when (> page-count (inc (:idx @state)))
                       (clear-stories)
                       (swap! state update :idx inc)
                       (render-stories))
        (= "return" k) (open-story)
        (= "left" k) (when (pos? (:page @state))
                       (clear-stories)
                       (swap! state update :page dec)
                       (swap! state assoc :idx 0)
                       (get-page-stories))
        (= "right" k) (when (< (:page @state) (.round js/Math (/ (count (:story-ids @state)) page-count)))
                        (clear-stories)
                        (swap! state update :page inc)
                        (swap! state assoc :idx 0)
                        (get-page-stories))))))

(defn setup-rl []
  (-> rl
    (.on "line"
      (fn [line]
        (.up charm 1)))
    (.on "close"
      (fn []
        (.close rl)
        (.exit js/process 0)))))

(defn init []
  ;; Top stories
  (setup-rl)
  (-> (rp "https://hacker-news.firebaseio.com/v0/topstories.json")
      (.then
        (fn [ids]
          (swap! state assoc :story-ids (json-> ids))
          (.on stdin "keypress" handle-events)
          (get-page-stories)))))

(init)
