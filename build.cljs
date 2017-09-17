(require '[lumo.build.api :as b])

(b/build "src"
  {:main 'hcknws.core
   :output-to "hcknws.js"
   :optimizations :simple
   :target :nodejs})

;; https://hacker-news.firebaseio.com/v0/topstories.json
