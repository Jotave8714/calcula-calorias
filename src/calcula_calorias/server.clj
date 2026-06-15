(ns calcula-calorias.server
  "Camada HTTP: rotas (Compojure), middleware JSON (ring-json) e o handler `app`.
  Comunicacao com o front-end via HTTP + JSON, conforme a especificacao."
  (:require [compojure.core :refer [defroutes POST GET]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.adapter.jetty :as jetty]
            [calcula-calorias.handlers :as h]))

(defroutes rotas
  ;; 1. Cadastrar / consultar dados pessoais
  (POST "/usuarios" {corpo :body} (h/registrar-usuario corpo))
  (GET  "/usuarios/:id" [id] (h/consultar-usuario id))

  ;; 2. Registrar consumo de alimento (ganho)
  (POST "/consumos" {corpo :body} (h/registrar-consumo corpo))

  ;; 3. Registrar atividade fisica (perda)
  (POST "/atividades" {corpo :body} (h/registrar-atividade corpo))

  ;; 4. Extrato de transacoes por periodo
  (GET "/extrato" [usuario-id inicio fim] (h/extrato usuario-id inicio fim))

  ;; 5. Saldo de calorias por periodo
  (GET "/saldo" [usuario-id inicio fim] (h/saldo usuario-id inicio fim))

  (route/not-found {:status 404 :body {:erro "Rota nao encontrada"}}))

(def app
  (-> rotas
      (wrap-json-body {:keywords? true})
      wrap-json-response
      wrap-keyword-params
      wrap-params))

(defn iniciar!
  "Inicia o servidor Jetty na porta indicada (default 3000)."
  ([] (iniciar! 3000))
  ([porta]
   (println (str "API Calculadora de Calorias em http://localhost:" porta))
   (jetty/run-jetty app {:port porta :join? true})))
