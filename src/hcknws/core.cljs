(ns hcknws.core
  (:require [cljs.nodejs :as node]
            [cljs.pprint :refer [pprint]]
            [hcknws.ui :as ui]
            [hcknws.prefs :as prefs :refer [set-prefs get-prefs]]
            [hcknws.utils :refer [json-> js->]]
            [clojure.string :refer [replace-first]]))

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
(def story-height 2)
(def types
  {:top {:label "Top"
         :path "topstories"}
   :new {:label "New"
         :path "newstories"}
   :best {:label "Best"
          :path "beststories"}})

(def state (atom {:idx 0
                  :page 0
                  :story-ids nil
                  :stories nil
                  :page-count 6
                  :type :top
                  :fetching true}))

(defn get-story-ids []
  (let [start (* (:page @state) (:page-count @state))]
    (subvec (:story-ids @state) start (min (count (:story-ids @state)) (+ start (:page-count @state))))))

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

(defn render-stories []
  (.write charm (ui/print-meta state))
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
        (= "down" k) (when (> (:page-count @state) (inc (:idx @state)))
                       (clear-stories)
                       (swap! state update :idx inc)
                       (render-stories))
        (= "return" k) (open-story)
        (= "left" k) (when (pos? (:page @state))
                       (clear-stories)
                       (swap! state update :page dec)
                       (swap! state assoc :idx 0)
                       (get-page-stories))
        (= "right" k) (when (< (:page @state) (.round js/Math (/ (count (:story-ids @state)) (:page-count @state))))
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
        (.cursor charm true)
        (.close rl)
        (.exit js/process 0)))))

(defn process-args []
  (doall
    (map
      (fn [arg]
        (cond
          (re-find #"--type=|-t=" arg) (set-prefs "type" (replace-first arg #"^.*=" ""))
          (re-find #"--per-page=|-p=" arg) (set-prefs "per_page" (replace-first arg #"^.*=" ""))))
      (nthrest (.slice (.-argv js/process) 3) 2))))

(defn load-prefs []
  (when-let [pc (get-prefs "per_page")]
    (swap! state assoc :page-count (js/parseInt pc 10)))
  (when-let [t (get-prefs "type")]
    (swap! state assoc :type (keyword t))))

(defn init []
  (.reset charm)
  (.cursor charm false)
  (process-args)
  (load-prefs)
  (setup-rl)
  (let [type (get types (:type @state))]
    (ui/print-banner (:label type))
    (-> (rp (str "https://hacker-news.firebaseio.com/v0/" (:path type) ".json"))
        (.then
          (fn [ids]
            (swap! state assoc :story-ids (json-> ids))
            (.on stdin "keypress" handle-events)
            (get-page-stories))))))

(init)
