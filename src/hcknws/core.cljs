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
(def types {:top {:label "Top"
                  :path "topstories"}
            :new {:label "New"
                  :path "newstories"}
            :best {:label "Best"
                   :path "beststories"}})

(def state (atom {:idx 0
                  :page 0
                  :story-ids nil
                  :stories nil
                  :page-count 8
                  :type :top
                  :display :normal
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

(defn open-link [type]
  (let [story (nth (:stories @state) (:idx @state))
        link (cond
              (= :comments type) (str "https://news.ycombinator.com/item?id=" (:id story))
              (= :url type) (:url story))]
    (spawn "open" (clj->js [link "--background"]))))

(defn clear-stories [y]
  (.position charm 0 y)
  (.erase charm "down"))

(defn render-stories []
  (let [max-c-w (count (str (apply max (map :descendants (:stories @state)))))]
    (doall
      (map-indexed
        (fn [idx {:keys [title] :as story}]
          (.write charm (ui/print-story max-c-w story (:display @state) (= idx (:idx @state)))))
        (:stories @state)))
    (ui/print-footer)))

(defn get-page-stories []
  (clear-stories 3)
  (.write charm (ui/print-meta state))
  (-> (get-stories)
      (.then
        (fn [stories]
          (swap! state assoc :stories stories)
          (render-stories)))))

(defn exit []
  (.cursor charm true)
  (.close rl)
  (.erase charm "line")
  (.log js/console "\nBye, see you in a bit.")
  (.exit js/process 0))

(defn handle-events [_ key]
  (let [story-change (fn [dir]
                        (clear-stories 6)
                        (swap! state update :idx dir)
                        (render-stories))
        page-change (fn [dir]
                      (swap! state update :page dir)
                      (swap! state assoc :idx 0)
                      (get-page-stories))]
    (when-not (:fetching @state)
      (let [k (.-name key)]
        (cond
          (and (= "up" k) (pos? (:idx @state)))
          (story-change dec)

          (and (= "down" k) (> (:page-count @state) (inc (:idx @state))))
          (story-change inc)

          (= "return" k)
          (open-link :url)

          (= "space" k)
          (open-link :comments)

          (= "q" k)
          (exit)

          (and (= "left" k) (pos? (:page @state)))
          (page-change dec)

          (and (= "right" k)
               (< (:page @state) (.round js/Math (/ (count (:story-ids @state))
                                                    (:page-count @state)))))
          (page-change inc))))))

(defn setup-rl []
  (-> rl
    (.on "line"
      (fn [line]
        (.up charm 1)))
    (.on "close" exit)))

(defn process-args []
  (doall
    (map
      (fn [arg]
        (cond
          (re-find #"--compact|-c" arg) (set-prefs "display" "compact")
          (re-find #"--normal|-n" arg) (set-prefs "display" "normal")
          (re-find #"--type=|-t=" arg) (set-prefs "type" (replace-first arg #"^.*=" ""))
          (re-find #"--per-page=|-p=" arg) (set-prefs "per_page" (replace-first arg #"^.*=" ""))))
      (nthrest (.slice (.-argv js/process) 3) 2))))

(defn load-prefs []
  (when-let [pc (get-prefs "per_page")]
    (swap! state assoc :page-count (js/parseInt pc 10)))
  (when-let [t (get-prefs "type")]
    (swap! state assoc :type (keyword t)))
  (when-let [d (get-prefs "display")]
    (swap! state assoc :display (keyword d))))

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
