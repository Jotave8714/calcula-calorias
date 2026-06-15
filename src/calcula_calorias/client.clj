(ns calcula-calorias.client
  "Front-end: cliente CLI da API. Comunica-se com o back-end via HTTP + JSON.
  O menu e implementado por RECURSAO DE CAUDA (recur), sem lacos imperativos."
  (:require [clj-http.client :as http]
            [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Entrada de dados
;; ---------------------------------------------------------------------------

(defn- ler [rotulo]
  (print (str rotulo ": "))
  (flush)
  (read-line))

(defn- parse-num [s]
  (try (Double/parseDouble s) (catch Exception _ nil)))

(defn- ler-num [rotulo]
  (if-let [n (parse-num (ler rotulo))]
    n
    (do (println "  Valor invalido, tente novamente.")
        (recur rotulo))))

;; ---------------------------------------------------------------------------
;; Chamadas HTTP
;; ---------------------------------------------------------------------------

(defn- post! [base caminho corpo]
  (http/post (str base caminho)
             {:content-type :json
              :form-params corpo
              :as :json
              :throw-exceptions false}))

(defn- get! [base caminho query]
  (http/get (str base caminho)
            {:query-params query
             :as :json
             :throw-exceptions false}))

(defn- fmt-transacao [t]
  (format "    #%s  %s  [%s/%s]  %s  ->  %.1f kcal"
          (:id t) (:data t) (:tipo t) (:categoria t)
          (:descricao t) (double (:calorias t))))

(defn- mostrar
  "Exibe a resposta da API de forma legivel, conforme o formato do corpo."
  [resp]
  (let [{:keys [status body]} resp]
    (cond
      (not (<= 200 status 299))
      (println "  ERRO" status "-" (or (:erro body) body))

      (contains? body :transacoes)
      (let [ts (:transacoes body)]
        (println (str "  Extrato (" (count ts) " transacao(oes)):"))
        (println (if (empty? ts)
                   "    (nenhuma transacao no periodo)"
                   (str/join "\n" (map fmt-transacao ts)))))

      (contains? body :saldo)
      (println (format "  Ganho: %.1f kcal | Perda: %.1f kcal | SALDO: %.1f kcal"
                       (double (:ganho body)) (double (:perda body)) (double (:saldo body))))

      (contains? body :tipo)
      (println "  Registrado:\n" (fmt-transacao body))

      (contains? body :id)
      (println (format "  Usuario #%s: %s | %s cm | %s kg | %s anos | sexo %s"
                       (:id body) (:nome body) (:altura body) (:peso body)
                       (:idade body) (:sexo body)))

      :else (println "  OK:" body))))

;; ---------------------------------------------------------------------------
;; Acoes do menu
;; ---------------------------------------------------------------------------

(defn- cadastrar-usuario [base]
  (mostrar (post! base "/usuarios"
                  {:nome (ler "Nome")
                   :altura (ler-num "Altura (cm)")
                   :peso (ler-num "Peso (kg)")
                   :idade (ler-num "Idade")
                   :sexo (ler "Sexo (M/F)")})))

(defn- consultar-usuario [base]
  (mostrar (http/get (str base "/usuarios/" (ler "Id do usuario"))
                     {:as :json :throw-exceptions false})))

(defn- registrar-consumo [base]
  (mostrar (post! base "/consumos"
                  {:usuario-id (ler-num "Id do usuario")
                   :alimento (ler "Alimento (em ingles, ex: banana)")
                   :data (ler "Data (YYYY-MM-DD)")
                   :quantidade (ler-num "Quantidade (g)")})))

(defn- registrar-atividade [base]
  (mostrar (post! base "/atividades"
                  {:usuario-id (ler-num "Id do usuario")
                   :atividade (ler "Atividade (em ingles, ex: skiing)")
                   :data (ler "Data (YYYY-MM-DD)")
                   :duracao (ler-num "Duracao (min)")})))

(defn- consultar-extrato [base]
  (mostrar (get! base "/extrato"
                 {:usuario-id (ler "Id do usuario")
                  :inicio (ler "Data inicial (YYYY-MM-DD)")
                  :fim (ler "Data final (YYYY-MM-DD)")})))

(defn- consultar-saldo [base]
  (mostrar (get! base "/saldo"
                 {:usuario-id (ler "Id do usuario")
                  :inicio (ler "Data inicial (YYYY-MM-DD)")
                  :fim (ler "Data final (YYYY-MM-DD)")})))

(def ^:private acoes
  {"1" cadastrar-usuario
   "2" consultar-usuario
   "3" registrar-consumo
   "4" registrar-atividade
   "5" consultar-extrato
   "6" consultar-saldo})

(defn- menu []
  (println "\n=== Calculadora de Calorias ===")
  (println "1) Cadastrar dados pessoais")
  (println "2) Consultar dados pessoais")
  (println "3) Registrar consumo de alimento")
  (println "4) Registrar atividade fisica")
  (println "5) Consultar extrato (por periodo)")
  (println "6) Consultar saldo (por periodo)")
  (println "0) Sair"))

(defn iniciar!
  "Loop principal do front-end por recursao de cauda."
  [base]
  (menu)
  (let [opcao (ler "Opcao")]
    (cond
      (= opcao "0") (println "Ate logo!")
      (contains? acoes opcao) (do (try ((acoes opcao) base)
                                       (catch Exception e
                                         (println "  Falha:" (.getMessage e))))
                                  (recur base))
      :else (do (println "  Opcao invalida.") (recur base)))))
