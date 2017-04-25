(ns liiteri.file-metadata-store-test
  (:require [clojure.test :refer :all]
            [liiteri.db.file-metadata-store :as metadata-store]))

(deftest sanitize-filename
  (doseq [[filename expected] [["parrot.png" "parrot.png"]
                               ["parrot..png" "parrot..png"]
                               ["parrot/:;.png" "parrot.png"]
                               ["p a r r o t . p n g" "p a r r o t . p n g"]
                               ["paRRot1234.png" "paRRot1234.png"]
                               ["pa::::-_rrot.png" "pa-_rrot.png"]
                               ["pärrötå.png" "parrota.png"]
                               ["parrot🍺.png" "parrot.png"]
                               [":::.png" "liite.png"]
                               ["nfd-ä.png" "nfd-a.png"]
                               [".png" "liite.png"]]]
    (let [actual (#'metadata-store/sanitize filename)]
      (is (= expected actual)))))

(deftest unicode-normalize-filename
  (doseq [[filename expected] [["nfd-ä.png" "nfd-ä.png"]
                               ["nfc-ä.png" "nfc-ä.png"]]]
    (let [actual (#'metadata-store/unicode-normalize filename)]
      (is (= expected actual)))))
