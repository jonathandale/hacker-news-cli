(ns hcknws.list-render
  (:require [cljs.nodejs :as node]))

(def readline (node/require "readline"))
(def charm ((node/require "charm") (.-stdout js/process)))
(def rl (.createInterface readline (.-stdin js/process) (.-stdout js/process)))
(def child (node/require "child_process"))
(def spawn (.-spawn child))

(def choices ["http://bbc.co.uk" "http://cnn.com"])
(def state (atom {:idx 0 :page 0}))

(defn clear-choices []
  (.erase charm "line")
  (dotimes [n (count choices)]
    (do
      (.up charm 1)
      (.erase charm "line"))))

(defn render-choices []
  (clear-choices)
  (doall
    (map-indexed
      (fn [idx choice]
        (.foreground charm "cyan")
        (.write charm (str "[" (if (= idx (:idx @state)) "X" " ") "]"))
        (.write charm (str choice \newline))
        (.foreground charm "white"))
      choices)))


(defn open-story [url]
  ; (spawn "open" (clj->js [(nth choices (:idx @state)) "--background"])))
  (spawn "open" (clj->js [url "--background"])))

(defn on-keypress [_ key]
  (let [k (.-name key)]
    (cond
      (= "up" k) (when (pos? (:idx @state))
                    (swap! state update :idx dec)
                    (render-choices))
      (= "down" k) (when (> (count choices) (inc (:idx @state)))
                     (swap! state update :idx inc)
                     (render-choices)))))
      ; (= "return" k) (open-story))))

(defn setup-rl []
  (-> rl
    (.on "line"
      (fn [line]
        (.up charm 1)))
        ; (.write charm (str "You chose" \newline))
        ; (.exit js/process 0)))
    (.on "close"
      (fn []
        (.log js/console "See ya in a bit.")
        (.close rl)
        (.exit js/process 0)))))

(defn setup []
  (.on (.-stdin js/process) "keypress" on-keypress)
  (render-choices)
  (setup-rl))
