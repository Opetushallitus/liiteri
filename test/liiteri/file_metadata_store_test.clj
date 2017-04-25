(ns liiteri.file-metadata-store-test
  (:require [clojure.test :refer :all]
            [liiteri.db.file-metadata-store :as metadata-store]))

(deftest normalize-filename
  (doseq [[filename expected] [["parrot.png" "parrot.png"]
                               ["parrot..png" "parrot..png"]
                               ["parrot/:;.png" "parrot.png"]
                               ["p a r r o t . p n g" "parrot.png"]]]
    (let [actual (#'metadata-store/normalize filename)]
      (is (= expected actual)))))
