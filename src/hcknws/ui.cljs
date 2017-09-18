(ns hcknws.ui
  (:require [cljs.nodejs :as node]))

(def chalk (node/require "chalk"))
(def log-update (node/require "log-update"))
(def timeout (atom nil))
(def dots {:interval 80 :frames ["⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧" "⠇" "⠏"]})

(def hn-orange (.rgb chalk 255 102 0))
(def hn-beige (.rgb chalk 246 246 239))

(defn start-spinner [text]
  (let [tick (atom 0)
        dot-len (count (:frames dots))]
    (reset! timeout
      (js/setInterval
        #(log-update (str (.cyan chalk (nth (:frames dots) (mod (swap! tick inc) dot-len))) " " text))
        (:interval dots)))))

(defn stop-spinner []
  (js/clearInterval @timeout)
  (reset! timeout nil)
  (log-update ""))

(defn log [& args]
  (apply log-update args))

(defn pstr [& args]
  (println (apply str args)))

(defn nl
  ([]
   (nl 1))
  ([n]
   (apply str (take n (repeat \newline)))))

(defn print-compact [{:keys [score by title descendants time] :as story} selected]
  (pstr
    (if selected (.white.underline chalk title) (.white chalk title))))

(defn print-normal [{:keys [score by title descendants time] :as story} selected]
  (pstr "will be normal"))

(defn print-story [story display selected]
  (if (= :compact display)
    (print-compact story selected)
    (print-normal story selected)))

(defn print-banner [label]
  (pstr
    ((.bgRgb chalk 255 102 0) " Hacker News ")
    (.white.inverse chalk (str " " label " Stories "))))

(defn print-meta [state]
  (str "Page " (inc (:page @state)) " of " (.round js/Math (/ (count (:story-ids @state)) (:page-count @state)))
       (nl)))
