(defproject calcula-calorias "0.1.0-SNAPSHOT"
  :description "Calculadora de Calorias - API (back-end) e cliente CLI (front-end) em Clojure"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.11.0"]          ; core HTTP
                 [ring/ring-jetty-adapter "1.11.0"] ; servidor Jetty
                 [compojure "1.7.1"]                ; roteamento
                 [ring/ring-json "0.5.1"]           ; middleware JSON
                 [cheshire "5.12.0"]                ; serializar/parsear JSON
                 [clj-http "3.12.3"]]               ; cliente HTTP (APIs externas + front-end)
  :main ^:skip-aot calcula-calorias.core
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler calcula-calorias.server/app}      ; ponto de entrada para o servidor
  :aliases {"servidor" ["run" "-m" "calcula-calorias.core" "servidor"]
            "cliente"  ["run" "-m" "calcula-calorias.core" "cliente"]}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
