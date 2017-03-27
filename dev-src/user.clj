(ns user
  (:require [reloaded.repl :refer [set-init! system init start stop go reset reset-all]]
            [schema.core :as s]))

(set-init! #(do
              (require 'liiteri.core)
              ((resolve 'liiteri.core/new-system))))
