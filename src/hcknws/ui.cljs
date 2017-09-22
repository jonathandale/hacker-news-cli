(ns hcknws.ui
  (:require [cljs.nodejs :as node]
            [cljs.pprint :refer [pprint]]))

(def chalk (node/require "chalk"))
(def charm ((node/require "charm") (.-stdout js/process)))
(def moment (node/require "moment"))
(def hn-orange (.rgb chalk 255 102 0))
(def hn-orange-bg (.bgRgb chalk 255 102 0))
(def light-grey (.rgb chalk 150 150 150))
(def timeout (atom nil))
(def dots {:interval 80 :frames ["⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧" "⠇" "⠏"]})
(def padding-left "    ")

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
    (if selected (hn-orange " ➜  ") padding-left)
    (.white chalk title)
    (nl)
    padding-left
    (.grey chalk (str "by " by " "))
    (light-grey (str (.fromNow (.unix moment time)) " "))
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
    (nl 2)
    padding-left
    (hn-orange-bg " Hacker News ")
    (.white.inverse chalk (str " " label " Stories "))
    (nl)))

(defn print-footer []
  (pstr
    (nl)
    padding-left
    (.grey chalk "'esc' or 'q' to exit, 'hcknws -h' for help.")))

(defn print-meta [state]
  (pstr
    (nl)
    padding-left
    "Page " (inc (:page @state))
    " of " (.round js/Math (/ (count (:story-ids @state)) (:page-count @state)))
    (nl)))

(defn print-help []
  (pstr
    (nl)
    "  hcknws [options]" (nl 2)
    (light-grey "  Options:  (display & type will be saved for next time)")
    (nl 2)

    "    -h, --help        output usage information" (nl)
    "    -d, --display     display mode [compact, normal]" (.cyan chalk " defaults to normal") (nl)
    "    -t, --type        story type [top, best, new]" (.cyan chalk " defaults to top") (nl 2)
    (light-grey "  Example:") (nl 2)
    (.cyan chalk "    Show compact view of best stories ") (nl)
    "    hcknws -d compact --type best" (nl 2)
    (light-grey "  Navigation:") (nl 2)
    "    - Use up/down/left/right arrows to navigate and paginate stories" (nl)
    "    - To visit the url of a story, press 'o' when on a selected story" (nl)
    "    - To visit the comments of a story, press 'c' when on a selected story" (nl))
  (.exit js/process 0))

(defn start-spinner []
  (let [tick (atom 0)
        dot-len (count (:frames dots))]
    (reset! timeout
      (js/setInterval
        #(-> charm
           (.position 33 3)
           (.write (str (hn-orange (nth (:frames dots) (mod (swap! tick inc) dot-len))))))
        (:interval dots)))))

(defn stop-spinner [y]
  (js/clearInterval @timeout)
  (-> charm
    (.left 1)
    (.erase "end")
    (.position 0 y))
  (reset! timeout nil))
