(ns calcula-calorias.config
  "Carrega variaveis de ambiente de um arquivo .env (na raiz do projeto).
  Prioridade: variavel real do SO > .env > default. Sem libs externas."
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(defn- parse-linha [m linha]
  (let [l (str/trim linha)]
    (if (or (str/blank? l) (str/starts-with? l "#"))
      m
      (let [[k v] (str/split l #"=" 2)]
        (assoc m (str/trim k) (str/trim (or v "")))))))

(defn carregar-env
  "Le o .env e devolve um mapa {\"CHAVE\" \"valor\"}. Vazio se nao existir."
  ([] (carregar-env ".env"))
  ([caminho]
   (let [f (io/file caminho)]
     (if (.exists f)
       (reduce parse-linha {} (str/split-lines (slurp f)))
       {}))))

(def ^:private env (delay (carregar-env)))

(defn valor
  "Valor da variavel: SO tem prioridade, depois .env, depois `default`."
  [chave default]
  (or (System/getenv chave) (get @env chave) default))
