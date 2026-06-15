(ns calcula-calorias.db
  "Base de dados em memoria (atom) e funcoes PURAS de manipulacao do estado.

  As funcoes puras recebem o mapa de estado e devolvem um NOVO mapa, sem
  efeitos colaterais. As funcoes com sufixo `!` aplicam essas transformacoes
  ao atom `estado` via `swap!`/`reset!`.

  Conforme a especificacao:
  - cada transacao e um hash-map;
  - as transacoes sao guardadas numa lista (`list`);
  - o estado e mantido em memoria com um atomo (`atom`).")

(def estado-inicial
  {:proximo-usuario-id 1
   :proximo-transacao-id 1
   :usuarios {}          ; id (long) -> mapa do usuario
   :transacoes '()})     ; lista de hash-maps (transacoes)

(defonce estado (atom estado-inicial))

;; ---------------------------------------------------------------------------
;; Funcoes puras: (estado -> novo-estado) ou (estado -> consulta)
;; ---------------------------------------------------------------------------

(defn criar-usuario
  "Devolve [novo-estado usuario-com-id]. Atribui um id sequencial ao usuario."
  [st usuario]
  (let [id (:proximo-usuario-id st)
        registro (assoc usuario :id id)]
    [(-> st
         (assoc-in [:usuarios id] registro)
         (update :proximo-usuario-id inc))
     registro]))

(defn buscar-usuario
  "Usuario pelo id, ou nil."
  [st id]
  (get-in st [:usuarios id]))

(defn adicionar-transacao
  "Devolve [novo-estado transacao-com-id]. A transacao e inserida no inicio
  da lista de transacoes (conj numa list = prepend)."
  [st transacao]
  (let [id (:proximo-transacao-id st)
        registro (assoc transacao :id id)]
    [(-> st
         (update :transacoes conj registro)
         (update :proximo-transacao-id inc))
     registro]))

(defn no-periodo?
  "Predicado: a data ISO (YYYY-MM-DD) esta no intervalo [inicio, fim]?
  Datas ISO comparam-se corretamente como strings. inicio/fim nil = aberto."
  [inicio fim data]
  (and (or (nil? inicio) (not (neg? (compare data inicio))))
       (or (nil? fim)    (not (pos? (compare data fim))))))

(defn transacoes-periodo
  "Lista (filtrada por funcao de ordem superior) das transacoes de um usuario
  num periodo. Ordenada por data crescente."
  [st {:keys [usuario-id inicio fim]}]
  (->> (:transacoes st)
       (filter #(= (:usuario-id %) usuario-id))
       (filter #(no-periodo? inicio fim (:data %)))
       (sort-by :data)))

(defn calcular-saldo
  "Saldo calorico = total de ganhos (alimentos) - total de perdas (atividades).
  Devolve um mapa com ganho, perda e saldo. Usa reduce (sem loops imperativos)."
  [transacoes]
  (let [{:keys [ganho perda]}
        (reduce (fn [acc {:keys [tipo calorias]}]
                  (update acc (if (= tipo "ganho") :ganho :perda) + calorias))
                {:ganho 0.0 :perda 0.0}
                transacoes)]
    {:ganho ganho
     :perda perda
     :saldo (- ganho perda)}))

;; ---------------------------------------------------------------------------
;; Wrappers com efeito: aplicam as funcoes puras ao atom
;; ---------------------------------------------------------------------------

(defn registrar-usuario!
  "Cadastra usuario no atom; devolve o registro criado (com :id)."
  [usuario]
  (let [[novo registro] (criar-usuario @estado usuario)]
    (reset! estado novo)
    registro))

(defn registrar-transacao!
  "Insere transacao no atom; devolve o registro criado (com :id)."
  [transacao]
  (let [[novo registro] (adicionar-transacao @estado transacao)]
    (reset! estado novo)
    registro))

(defn resetar!
  "Limpa o estado (util para testes)."
  []
  (reset! estado estado-inicial))
