(ns liiteri.utils)

(defn not-blank? [string]
  (not (clojure.string/blank? string)))
