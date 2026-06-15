(ns calcula-calorias.handlers
  "Logica de negocio dos endpoints. Cada handler recebe dados ja parseados
  (mapas com chaves keyword) e devolve um mapa de resposta Ring
  {:status _ :body _}. A serializacao JSON e feita pelo middleware."
  (:require [calcula-calorias.db :as db]
            [calcula-calorias.external :as ext]))

(defn- resp [status body] {:status status :body body})
(defn- ok [body] (resp 200 body))
(defn- criado [body] (resp 201 body))
(defn- erro [status msg] (resp status {:erro msg}))

(defn- faltando
  "Devolve a primeira chave obrigatoria ausente/vazia em `m`, ou nil."
  [m chaves]
  (some (fn [k] (when (nil? (get m k)) k)) chaves))

(defn- ->long [x]
  (cond
    (number? x) (long x)
    (string? x) (try (Long/parseLong x) (catch Exception _ nil))
    :else nil))

;; ---------------------------------------------------------------------------
;; 1. Dados pessoais
;; ---------------------------------------------------------------------------

(defn registrar-usuario [{:keys [nome altura peso idade sexo] :as corpo}]
  (if-let [k (faltando corpo [:altura :peso :idade :sexo])]
    (erro 400 (str "Campo obrigatorio ausente: " (name k)))
    (criado (db/registrar-usuario!
             {:nome nome :altura altura :peso peso :idade idade :sexo sexo}))))

(defn consultar-usuario [id]
  (if-let [u (db/buscar-usuario @db/estado (->long id))]
    (ok u)
    (erro 404 "Usuario nao encontrado")))

;; ---------------------------------------------------------------------------
;; 2. Consumo de alimento (ganho calorico) via USDA FDC
;; ---------------------------------------------------------------------------

(defn registrar-consumo [{:keys [usuario-id alimento data quantidade] :as corpo}]
  (if-let [k (faltando corpo [:usuario-id :alimento :data :quantidade])]
    (erro 400 (str "Campo obrigatorio ausente: " (name k)))
    (if-not (db/buscar-usuario @db/estado (->long usuario-id))
      (erro 404 "Usuario nao encontrado")
      (try
        (let [{:keys [descricao calorias]} (ext/calorias-alimento alimento quantidade)]
          (criado (db/registrar-transacao!
                   {:usuario-id (->long usuario-id)
                    :tipo "ganho"
                    :categoria "alimento"
                    :descricao descricao
                    :data data
                    :quantidade quantidade
                    :calorias calorias})))
        (catch clojure.lang.ExceptionInfo e
          (erro 502 (.getMessage e)))))))

;; ---------------------------------------------------------------------------
;; 3. Atividade fisica (perda calorica) via API Ninjas
;; ---------------------------------------------------------------------------

(defn registrar-atividade [{:keys [usuario-id atividade data duracao] :as corpo}]
  (if-let [k (faltando corpo [:usuario-id :atividade :data :duracao])]
    (erro 400 (str "Campo obrigatorio ausente: " (name k)))
    (if-let [usuario (db/buscar-usuario @db/estado (->long usuario-id))]
      (try
        (let [{:keys [descricao calorias]}
              (ext/calorias-atividade atividade duracao (:peso usuario))]
          (criado (db/registrar-transacao!
                   {:usuario-id (->long usuario-id)
                    :tipo "perda"
                    :categoria "atividade"
                    :descricao descricao
                    :data data
                    :duracao duracao
                    :calorias calorias})))
        (catch clojure.lang.ExceptionInfo e
          (erro 502 (.getMessage e))))
      (erro 404 "Usuario nao encontrado"))))

;; ---------------------------------------------------------------------------
;; 4 e 5. Extrato e saldo por periodo
;; ---------------------------------------------------------------------------

(defn- consulta-periodo [usuario-id inicio fim]
  {:usuario-id (->long usuario-id) :inicio inicio :fim fim})

(defn extrato [usuario-id inicio fim]
  (if-not (->long usuario-id)
    (erro 400 "Parametro usuario-id invalido")
    (ok {:transacoes (db/transacoes-periodo
                      @db/estado (consulta-periodo usuario-id inicio fim))})))

(defn saldo [usuario-id inicio fim]
  (if-not (->long usuario-id)
    (erro 400 "Parametro usuario-id invalido")
    (let [txs (db/transacoes-periodo
               @db/estado (consulta-periodo usuario-id inicio fim))]
      (ok (merge {:periodo {:inicio inicio :fim fim}}
                 (db/calcular-saldo txs))))))
