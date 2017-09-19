(ns hcknws.ui
  (:require [cljs.nodejs :as node]
            [cljs.pprint :refer [pprint]]))

(def chalk (node/require "chalk"))
(def hn-orange (.rgb chalk 255 102 0))
(def hn-orange-bg (.bgRgb chalk 255 102 0))
(def hn-beige (.rgb chalk 246 246 239))
(def padding-left " ")

(defn pstr [& args]
  (str (println (apply str args))))

(defn nl
  ([]
   (nl 1))
  ([n]
   (apply str (take n (repeat \newline)))))

(defn print-compact [max-w {:keys [type title descendants] :as story} selected]
  (pstr
    padding-left
    (hn-orange (str (if (= "job" type) (.yellow chalk "job") descendants)))
    (apply str (take (- max-w (if (= "job" type) 3 (count (str descendants)))) (repeatedly #(str " "))))
    "  "
    (if selected (.white.underline chalk title) (.white chalk title))))

(defn print-normal [{:keys [score by title descendants time] :as story} selected]
  (pstr "will be normal"))

(defn print-story [max-w story display selected]
  (if (= :compact display)
    (print-compact max-w story selected)
    (print-normal story selected)))

(defn print-banner [label]
  (pstr
    (nl)
    padding-left
    (hn-orange-bg " Hacker News ")
    (.white.inverse chalk (str " " label " Stories "))))

(defn print-footer []
  (pstr
    (nl)
    padding-left
    (.grey chalk "`ctrl+c` to exit, or type 'q'")))

(defn print-meta [state]
  (pstr
    (nl)
    padding-left
    "Page " (inc (:page @state))
    " of " (.round js/Math (/ (count (:story-ids @state)) (:page-count @state)))
    (nl)))
