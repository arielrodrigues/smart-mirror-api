(ns smart-mirror.integration.time-test
  (:require [common.http-client :as http]
            [smart-mirror.integration.setup :refer [defflow]]
            [state-flow.api :as flow :refer [flow]]
            [state-flow.assertions.matcher-combinators :refer [match?]]))

(defflow get-time-test
  (flow "Given someone requested the time"
        (let [{:keys [body]} (http/request :get "/api/time")]
          (match?
           body
           {:a 1}))))
