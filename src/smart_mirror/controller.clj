(ns smart-mirror.controller
  (:require [common.exceptions]
            [smart-mirror.logic :as logic]
            [smart-mirror.time :as time]))

(defn foo
  [_request
   _context]
  (logic/foo))

(defn now
  [{:keys [query-params] :as _request}
   as-of]
  (let [now (time/->time as-of)
        {timezones :include :or {timezones []}} query-params
        invalid-timezones? (time/valid-timezone? timezones)]
    (if invalid-timezones?
      (common.exceptions/bad-request "invalid timezone")
      (time/time+zones now timezones))))
