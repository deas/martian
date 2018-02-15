(ns martian.interceptors-test
  (:require [martian.interceptors :as i]
            [cheshire.core :as json]
            [clojure.test :refer :all]
            [martian.encoders :as encoders]
            [martian.test-utils :refer [input-stream->byte-array]]
            [tripod.context :as tc]))

(deftest encode-body-test
  (let [i i/default-encode-body
        body {:the {:wheels ["on" "the" "bus"]}
              :go {:round {:and "round"}}}]
    (testing "json"
      (is (= {:body (json/encode body)
              :headers {"Content-Type" "application/json"}}
             (:request ((:enter i) {:request {:body body}
                                    :handler {:consumes ["application/json"]}})))))

    (testing "edn"
      (is (= {:body (pr-str body)
              :headers {"Content-Type" "application/edn"}}
             (:request ((:enter i) {:request {:body body}
                                    :handler {:consumes ["application/edn"]}})))))

    (testing "transit"
      (testing "+json"
        (is (= {:body body
                :headers {"Content-Type" "application/transit+json"}}
               (-> ((:enter i) {:request {:body body}
                                :handler {:consumes ["application/transit+json"]}})
                   :request
                   (update :body (comp #(encoders/transit-decode % :json)
                                       input-stream->byte-array))))))

      (testing "+msgpack"
        (is (= {:body body
                :headers {"Content-Type" "application/transit+msgpack"}}
               (-> ((:enter i) {:request {:body body}
                                :handler {:consumes ["application/transit+msgpack"]}})
                   :request
                   (update :body (comp #(encoders/transit-decode % :msgpack)
                                       input-stream->byte-array)))))))))

(defn- stub-response [content-type body]
  {:name ::stub-response
   :enter (fn [ctx]
            (assoc ctx :response {:body body
                                  :headers {:content-type content-type}}))})

(deftest coerce-response-test
  (let [i i/default-coerce-response
        body {:the {:wheels ["on" "the" "bus"]}
              :go {:round {:and "round"}}}]

    (testing "json"
      (let [ctx (tc/enqueue* {} [i (stub-response "application/json" (json/encode body))])]
        (is (= body
               (-> (tc/execute ctx) :response :body)))))

    (testing "edn"
      (let [ctx (tc/enqueue* {} [i (stub-response "application/edn" (pr-str body))])]
        (is (= body
               (-> (tc/execute ctx) :response :body)))))

    (testing "transit"
      (testing "+json"
        (let [ctx (tc/enqueue* {} [i (stub-response "application/transit+json"
                                                    (slurp (encoders/transit-encode body :json)))])]
          (is (= body
                 (-> (tc/execute ctx) :response :body)))))

      (testing "+json"
        (let [ctx (tc/enqueue* {} [i (stub-response "application/transit+msgpack"
                                                    (input-stream->byte-array (encoders/transit-encode body :msgpack)))])]
          (is (= body
                 (-> (tc/execute ctx) :response :body))))))))

(deftest custom-encoding-test
  (testing "a user can support an encoding that martian doesn't know about by default"
    (let [reverse-string #(apply str (reverse %))
          encoders (assoc (encoders/default-encoders)
                          "text/magical+json" {:encode (comp reverse-string json/encode)
                                               :decode (comp #(json/decode % keyword) reverse-string)
                                               :as :magic})
          body {:the {:wheels ["on" "the" "bus"]}
                :go {:round {:and "round"}}}
          encoded-body (-> body json/encode reverse-string)]

      (let [ctx (tc/enqueue* {:request {:body body}
                              :handler {:consumes ["text/magical+json"]
                                        :produces ["text/magical+json"]}}
                             [(i/encode-body encoders)
                              (i/coerce-response encoders)
                              (stub-response "text/magical+json" encoded-body)])
            result (tc/execute ctx)]

        (is (= {:body encoded-body
                :headers {"Content-Type" "text/magical+json"
                          "Accept" "text/magical+json"}
                :as :magic}
               (:request result)))

        (is (= {:body body
                :headers {:content-type "text/magical+json"}}
               (:response result)))))))

(deftest auto-encoder-test
  (testing "when the server speaks a language martian doesn't understand it leaves it alone"
    (let [reverse-string #(apply str (reverse %))
          body {:the {:wheels ["on" "the" "bus"]}
                :go {:round {:and "round"}}}
          encoded-body (-> body json/encode reverse-string)]

      (let [ctx (tc/enqueue* {:request {:body encoded-body
                                        :headers {"Content-Type" "text/magical+json"
                                                  "Accept" "text/magical+json"}}
                              :handler {:consumes ["text/magical+json"]
                                        :produces ["text/magical+json"]}}
                             [i/default-encode-body
                              i/default-coerce-response
                              (stub-response "text/magical+json" encoded-body)])
            result (tc/execute ctx)]

        (is (= {:body encoded-body
                :headers {"Content-Type" "text/magical+json"
                          "Accept" "text/magical+json"}
                :as :auto}
               (:request result)))

        (is (= {:body encoded-body
                :headers {:content-type "text/magical+json"}}
               (:response result)))))))

;; 1. write release notes confirming the breaking change
