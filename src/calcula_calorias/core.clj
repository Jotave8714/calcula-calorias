(ns calcula-calorias.core
  "Ponto de entrada. Despacha entre back-end (servidor) e front-end (cliente CLI):

      lein run servidor [porta]
      lein run cliente  [url-base]"
  (:require [calcula-calorias.server :as server]
            [calcula-calorias.client :as client])
  (:gen-class))

(defn -main
  [& args]
  (let [[modo arg] args]
    (case modo
      "servidor" (server/iniciar! (if arg (Long/parseLong arg) 3000))
      "cliente"  (client/iniciar! (or arg "http://localhost:3000"))
      (do (println "Uso: lein run <servidor|cliente> [porta|url]")
          (println "  servidor [porta]   inicia a API (default 3000)")
          (println "  cliente  [url]     inicia o front-end CLI (default http://localhost:3000)")))))
