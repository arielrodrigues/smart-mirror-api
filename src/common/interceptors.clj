(ns common.interceptors
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.interceptor.error :refer [error-dispatch]]))

(s/def ::name (s/nilable keyword?))
(s/def ::enter (s/nilable ifn?))
(s/def ::leave (s/nilable ifn?))
(s/def ::error (s/nilable ifn?))

(s/def ::interceptor (s/keys :req-un [::name
                                      ::enter
                                      ::leave
                                      ::error]))

(s/def ::interceptors (s/coll-of ::interceptor))

(def supported-content-types
  ["text/plain"
   "application/edn"
   "application/json"])

(defn- coerce-body [body content-type]
  (case content-type
    "text/plain" body
    "application/edn" (pr-str body)
    "application/json" (json/write-str body)
    body))

(def content-negotiation-interceptor
  (content-negotiation/negotiate-content supported-content-types))

(def coerce-body-interceptor
  {:name ::coerce-body
   :leave (fn [context]
            (let [default-content-type "application/json"
                  accepted-content-type (get-in context [:request :accept :field] default-content-type)
                  response (get context :response)
                  body (-> context :response :body)]
              (assoc context :response (assoc response
                                              :headers {"Content-Type" accepted-content-type}
                                              :body (coerce-body body accepted-content-type)))))})

(def service-error-handler
  (error-dispatch [context ex]

                  [{:exception-type :bad-request}]
                  (assoc context :response {:status 400 :body (-> ex Throwable->map :cause)})

                  [{:exception-type :not-found}]
                  (assoc context :response {:status 404 :body (-> ex Throwable->map :cause)})

                  :else
                  (assoc context :io.pedestal.interceptor.chain/error ex)))


(def common-interceptors
  [service-error-handler
   content-negotiation-interceptor
   coerce-body-interceptor])

(defn- inject-common-interceptors-on-path-map
  [path-map]
  (update-vals path-map (fn [v]
                          (let [handler (get v :handler)]
                            (assoc v :handler (conj common-interceptors handler))))))

(defn routes->routes+common-interceptors
  [routes]
  (->> routes
       (partition 2)
       (map (fn [[path path-map]]
              [path (inject-common-interceptors-on-path-map path-map)]))
       flatten))
