(ns hcknws.ui
  (:require [cljs.nodejs :as node]
            [cljs.pprint :refer [pprint]]))

(def chalk (node/require "chalk"))
(def charm ((node/require "charm") (.-stdout js/process)))
(def moment (node/require "moment"))
(def hn-orange (.rgb chalk 255 102 0))
(def hn-orange-bg (.bgRgb chalk 255 102 0))
(def hn-beige (.rgb chalk 246 246 239))
(def timeout (atom nil))
(def dots {:interval 80 :frames ["⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧" "⠇" "⠏"]})
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
  (pstr
    padding-left
    (if selected (.white.underline chalk title) (.white chalk title))
    (nl)
    padding-left
    (.grey chalk (str "by " by " "))
    ((.rgb chalk 150 150 150) (str (.fromNow (.unix moment time)) " "))
    (when descendants
      (str
        (hn-orange descendants) " "
        (hn-orange (if (= 1 descendants) "comment" "comments"))))
    (nl)))

(defn print-story [max-w story display selected]
  (if (= :compact display)
    (print-compact max-w story selected)
    (print-normal story selected)))

(defn print-banner [label]
  (pstr
    (nl)
    padding-left
    (hn-orange-bg " Hacker News ")
    (.white.inverse chalk (str " " label " Stories "))
    (nl)))

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

(defn start-spinner []
  (let [tick (atom 0)
        dot-len (count (:frames dots))]
    (reset! timeout
      (js/setInterval
        #(-> charm
           (.position 30 2)
           (.write (str (hn-orange (nth (:frames dots) (mod (swap! tick inc) dot-len))))))
        (:interval dots)))))

(defn stop-spinner [y]
  (js/clearInterval @timeout)
  (-> charm
    (.left 1)
    (.erase "end")
    (.position 0 y))
  (reset! timeout nil))
