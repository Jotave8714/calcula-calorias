(ns calcula-calorias.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [calcula-calorias.db :as db]))

(deftest criar-usuario-test
  (testing "atribui id sequencial e guarda no estado"
    (let [[st1 u1] (db/criar-usuario db/estado-inicial {:peso 70})
          [_   u2] (db/criar-usuario st1 {:peso 80})]
      (is (= 1 (:id u1)))
      (is (= 2 (:id u2)))
      (is (= u1 (db/buscar-usuario st1 1))))))

(deftest adicionar-transacao-test
  (testing "insere transacao com id na lista"
    (let [[st1 t1] (db/adicionar-transacao db/estado-inicial
                                           {:usuario-id 1 :tipo "ganho" :calorias 100.0})]
      (is (= 1 (:id t1)))
      (is (= 1 (count (:transacoes st1))))
      (is (list? (:transacoes st1))))))

(deftest no-periodo?-test
  (testing "comparacao de datas ISO inclusiva e com extremos abertos"
    (is (db/no-periodo? "2026-01-01" "2026-12-31" "2026-06-15"))
    (is (db/no-periodo? "2026-06-15" "2026-06-15" "2026-06-15"))
    (is (not (db/no-periodo? "2026-01-01" "2026-05-31" "2026-06-15")))
    (is (db/no-periodo? nil nil "2026-06-15"))))

(deftest transacoes-periodo-test
  (testing "filtra por usuario e periodo, ordenando por data"
    (let [st {:transacoes (list {:usuario-id 1 :data "2026-06-10" :tipo "ganho" :calorias 100.0}
                                {:usuario-id 1 :data "2026-06-20" :tipo "perda" :calorias 50.0}
                                {:usuario-id 2 :data "2026-06-15" :tipo "ganho" :calorias 999.0})}
          r (db/transacoes-periodo st {:usuario-id 1 :inicio "2026-06-01" :fim "2026-06-15"})]
      (is (= 1 (count r)))
      (is (= "2026-06-10" (:data (first r)))))))

(deftest calcular-saldo-test
  (testing "saldo = ganhos - perdas"
    (let [txs [{:tipo "ganho" :calorias 300.0}
               {:tipo "ganho" :calorias 200.0}
               {:tipo "perda" :calorias 150.0}]
          {:keys [ganho perda saldo]} (db/calcular-saldo txs)]
      (is (= 500.0 ganho))
      (is (= 150.0 perda))
      (is (= 350.0 saldo)))))
