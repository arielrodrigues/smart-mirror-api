(ns common.http-client
  (:require [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [common.system :refer [system]]
            [io.pedestal.test :refer [response-for]]))

(defrecord HttpClient []
  component/Lifecycle
  
  (start [component])
  (stop [component]))

(defn new-http-client []
  (map->HttpClient {}))

(defrecord MockHttpClient [mock-http-server]
  component/Lifecycle

  (start [component]
    (assoc component :service (get mock-http-server :service)))

  (stop [component]
    (dissoc component :service)))

(defn new-mock-http-client []
  (map->MockHttpClient {}))

(defn ->parse-body
  [{:keys [status body]}]
  {:status status :body (json/read-json body)})

(defn request
  [verb url & options]
  (if-let [service (some-> @system :http-client :service)]
    (->parse-body (apply response-for service verb url options))
    (throw (ex-info "Test system isn't initiated!" {}))))
