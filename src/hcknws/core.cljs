(ns hcknws.core
  (:require [cljs.nodejs :as node]
            [cljs.pprint :refer [pprint]]
            [hcknws.ui :as ui]
            [hcknws.prefs :as prefs :refer [set-prefs get-prefs]]
            [hcknws.utils :refer [json-> js->]]
            [clojure.string]
            [goog.functions :as gfuncs]))

(node/enable-util-print!)

(def minimist (node/require "minimist"))
(def argv (minimist (.slice (.-argv js/process) 2)))
(def rp (node/require "request-promise"))
(def promise (node/require "bluebird"))
(def readline (node/require "readline"))
(def stdout (.-stdout js/process))
(def stdin (.-stdin js/process))
(def rl (.createInterface readline stdin stdout))
(def charm ((node/require "charm") stdout))
(def spawn (.-spawn (node/require "child_process")))
(def chalk (node/require "chalk"))
(def size (node/require "window-size"))
(def header-height 7)
(def meta-height 3)
(def footer-height 3)
(def types {:top {:label "Top"
                  :path "topstories"}
            :new {:label "New"
                  :path "newstories"}
            :best {:label "Best"
                   :path "beststories"}})
(def display {:normal 3
              :compact 1})
(def state (atom {:idx 0
                  :page 0
                  :story-ids nil
                  :stories nil
                  :page-count 10
                  :story-type :top
                  :display :normal
                  :theme :dark
                  :fetching true}))

;; Calculate and set number of stories to show per page,
;; based on available vertical space, and story height.
(defn set-page-count []
  (let [avail-rows (- (:height (js-> (.get size)))
                      header-height
                      footer-height)
        page-count (.floor js/Math (/ avail-rows (get display (:display @state))))]
    (swap! state assoc :page-count page-count)))

;; Select story ids, based on page count and items/page.
(defn get-story-ids []
  (let [{:keys [page-count page story-ids]} @state
        start (* page page-count)]
    (subvec story-ids start (min (count story-ids) (+ start page-count)))))

(defn get-stories []
  (swap! state assoc :fetching true)
  (-> promise
      (.all (map #(rp (str "https://hacker-news.firebaseio.com/v0/item/" % ".json"))
              (get-story-ids)))
      (.then #(do
                (swap! state assoc :fetching false)
                (map json-> (js-> %))))))

;; Open comments or original story in browser (in the background).
(defn open-link [type]
  (let [story (nth (:stories @state) (:idx @state))
        c-link (str "https://news.ycombinator.com/item?id=" (:id story))
        link (cond
              (= :comments type) c-link
              (= :url type) (or (:url story) c-link))]
    (spawn "open" (clj->js [link "--background"]))))

(defn clear-stories [y]
  (.position charm 0 y)
  (.erase charm "down"))

(defn render-stories []
  (let [max-c-w (count (str (apply max (map :descendants (:stories @state)))))
        job-w (when (some #(= "job" (:story-type %)) (:stories @state)) 3)
        max-w (max max-c-w job-w)]
    (doall
      (map-indexed
        (fn [idx {:keys [title] :as story}]
          (.write charm (ui/print-story max-w story (:display @state) (= idx (:idx @state)) (:theme @state))))
        (:stories @state)))
    (ui/print-footer)))

;; Clear stories, show loading spinner, update meta, and once
;; stories are available, update state and then render.
(defn get-page-stories []
  (clear-stories (- header-height meta-height))
  (.write charm (ui/print-meta state))
  (ui/start-spinner)
  (-> (get-stories)
      (.then
        (fn [stories]
          (ui/stop-spinner header-height)
          (swap! state assoc :stories stories)
          (render-stories)))))

(defn exit
  ([]
   (exit "   Bye, see you in a bit."))
  ([msg]
   (-> charm (.cursor true) (.erase "line"))
   (.close rl)
   (.log js/console (str "\n " msg))
   (.exit js/process 0)))

;; Future improvement: It's not necessary to re-render list of stories
;; when highlighting the selected story (rendering arrow in the left gutter).
;; Instead, consider just clearing/rendering the arrow when navigating up/down.
(defn handle-events [_ key]
  (let [story-change (fn [dir]
                        (clear-stories header-height)
                        (swap! state update :idx dir)
                        (render-stories))
        page-change (fn [dir idx]
                      (swap! state update :page dir)
                      (swap! state assoc :idx idx)
                      (get-page-stories))]
    (when-not (:fetching @state)
      (let [k (.-name key)]
        (cond
          (= "up" k) (if (pos? (:idx @state))
                       (story-change dec)
                       (when (pos? (:page @state))
                         (page-change dec (dec (:page-count @state)))))

          (= "down" k) (if (> (:page-count @state) (inc (:idx @state)))
                         (story-change inc)
                         (page-change inc 0))

          (= "o" k)
          (do
            (.erase charm "line")
            (open-link :url))

          (= "c" k)
          (do
            (.erase charm "line")
            (open-link :comments))

          (= "b" k)
          (do
            (.erase charm "line")
            (open-link :url)
            (open-link :comments))

          (or (= "q" k) (= "escape" k))
          (exit)

          (and (= "left" k) (pos? (:page @state)))
          (page-change dec 0)

          (and (= "right" k)
               (< (:page @state) (.round js/Math (/ (count (:story-ids @state))
                                                    (:page-count @state)))))
          (page-change inc 0))))))

(defn setup-rl []
  (-> rl
    (.on "line"
      (fn [line]
        (.up charm 1)))
    (.on "close" exit)))

(defn set-and-validate-arg [type arg types]
  (if (some #(= arg %) types)
    (set-prefs type arg)
    (exit (str (ui/hn-orange (str arg " isn't a valid " type " option!"))
               "\n Should be one of: " (apply str (interpose ", " types))))))

;; Process command line args & validate. If invalid, exit.
;; Simplify when more prefs. DRY.
(defn process-args []
  (let [{:keys [d display t theme s story-type h help v version] :as args} (js-> argv)]
    (cond
      (or help h) (ui/print-help)
      :else
      (do
        (when-let [display* (or display d)]
          (set-and-validate-arg "display" display* ["compact" "normal"]))
        (when-let [story* (or story-type s)]
          (set-and-validate-arg "story-type" story* ["top" "best" "new"]))
        (when-let [theme* (or theme t)]
          (set-and-validate-arg "theme" theme* ["light" "dark"]))))))

;; Simplify when more prefs. DRY.
;; Get user settings from stored prefs, and add to state
(defn load-prefs []
  (when-let [s (get-prefs "story-type")]
    (swap! state assoc :story-type (keyword s)))
  (when-let [d (get-prefs "display")]
    (swap! state assoc :display (keyword d)))
  (when-let [t (get-prefs "theme")]
    (swap! state assoc :theme (keyword t))))

;; Need more thought about clearing/re-rendering on resize.
;; For now, bail (gracefully)
(defn handle-window-resize []
  (.on stdout "resize"
    (gfuncs/debounce
      (fn []
        (-> charm (.cursor true) (.erase "line"))
        (.log js/console
          (str (ui/nl)
               (.yellow chalk
                  (str ui/padding-left "Things get a bit messy when resizing... "
                                       "best to just start again."))))
        (.exit js/process 0))
      500)))

(defn init []
  (process-args)
  (load-prefs)
  (-> charm (.reset) (.cursor false))
  (setup-rl)
  (set-page-count)
  (handle-window-resize)
  (let [type (get types (:story-type @state))]
    (ui/print-banner (:label type) (:theme @state))
    (ui/start-spinner)
    (-> (rp (str "https://hacker-news.firebaseio.com/v0/" (:path type) ".json"))
        (.then
          (fn [ids]
            (ui/stop-spinner 3)
            (swap! state assoc :story-ids (json-> ids))
            (.on stdin "keypress" handle-events)
            (get-page-stories))))))

(init)
