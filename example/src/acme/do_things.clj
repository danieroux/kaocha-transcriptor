(ns acme.do-things)

(defn do-things
  []
  {:acme.things/done ["many" "things" "done!"]})

(defn do-things-badly
  []
  (println "Oops, no useful return"))

(defn do-things-exceptionally-badly
  []
  (throw (ex-info "Things done exceptionally badly" {::oops ::again})))