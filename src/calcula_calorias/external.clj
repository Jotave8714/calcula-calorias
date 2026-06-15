(ns calcula-calorias.external
  "Integracao com APIs externas (de terceiros) para obter as calorias:

  - USDA FoodData Central  -> calorias de um ALIMENTO (ganho calorico)
      https://fdc.nal.usda.gov/api-guide
  - API Ninjas Calories Burned -> calorias de uma ATIVIDADE fisica (perda calorica)
      https://api-ninjas.com/api/caloriesburned

  Chaves lidas de variaveis de ambiente:
  - USDA_API_KEY   (default: \"DEMO_KEY\")
  - API_NINJAS_KEY (obrigatoria para atividades)"
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [calcula-calorias.config :as cfg]))

(def usda-url "https://api.nal.usda.gov/fdc/v1/foods/search")
(def ninjas-url "https://api.api-ninjas.com/v1/caloriesburned")

(defn- usda-key [] (cfg/valor "USDA_API_KEY" "DEMO_KEY"))
(defn- ninjas-key [] (cfg/valor "API_NINJAS_KEY" nil))

(def ^:private kg->lb 2.20462)

(defn- energia-kcal
  "Valor de Energia (KCAL) por 100g na lista de foodNutrients, ou nil."
  [food-nutrients]
  (some (fn [{:keys [nutrientName unitName value]}]
          (when (and nutrientName
                     (str/starts-with? nutrientName "Energy")
                     (= unitName "KCAL"))
            value))
        food-nutrients))

(defn- primeiro-com-energia
  "Primeiro alimento da lista que possui Energia em KCAL; devolve [food kcal]."
  [foods]
  (some (fn [food]
          (when-let [kcal (energia-kcal (:foodNutrients food))]
            [food kcal]))
        foods))

(defn calorias-alimento
  "Consulta a USDA FDC e devolve um mapa com as calorias do alimento `nome`
  para `quantidade-g` gramas. O valor da USDA e por 100g, escalado pela
  quantidade. Lanca ex-info em caso de falha."
  [nome quantidade-g]
  (let [resp (http/get usda-url
                       {:query-params {:api_key (usda-key)
                                       :query nome
                                       :pageSize 25}
                        :as :json
                        :throw-exceptions false})]
    (when (>= (:status resp) 400)
      (throw (ex-info "Falha ao consultar USDA FoodData Central"
                      {:status (:status resp) :alimento nome})))
    (let [[food kcal-100g] (primeiro-com-energia (-> resp :body :foods))]
      (when-not kcal-100g
        (throw (ex-info "Alimento nao encontrado na USDA FDC" {:alimento nome})))
      {:descricao (:description food)
       :quantidade-g quantidade-g
       :kcal-por-100g kcal-100g
       :calorias (* kcal-100g (/ quantidade-g 100.0))})))

(defn calorias-atividade
  "Consulta a API Ninjas e devolve um mapa com as calorias gastas na atividade
  `nome` por `duracao-min` minutos, para um usuario de `peso-kg` quilos.
  Lanca ex-info em caso de falha ou ausencia de chave."
  [nome duracao-min peso-kg]
  (when-not (ninjas-key)
    (throw (ex-info "Variavel de ambiente API_NINJAS_KEY nao definida" {})))
  (let [resp (http/get ninjas-url
                       {:headers {"X-Api-Key" (ninjas-key)}
                        :query-params {:activity nome
                                       :weight (Math/round (double (* peso-kg kg->lb)))
                                       :duration duracao-min}
                        :as :json
                        :throw-exceptions false})]
    (when (>= (:status resp) 400)
      (throw (ex-info "Falha ao consultar API Ninjas Calories Burned"
                      {:status (:status resp) :atividade nome})))
    (let [atividade (-> resp :body first)]
      (when-not atividade
        (throw (ex-info "Atividade nao encontrada na API Ninjas" {:atividade nome})))
      {:descricao (:name atividade)
       :duracao-min duracao-min
       :calorias-por-hora (:calories_per_hour atividade)
       :calorias (double (:total_calories atividade))})))
