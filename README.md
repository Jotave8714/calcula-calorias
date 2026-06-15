# calcula-calorias

Calculadora de Calorias — projeto AV3 de Programação Funcional (Clojure).

Aplicação cliente/servidor que registra ganho calórico (consumo de alimentos) e perda
calórica (atividades físicas), exibindo extrato e saldo por período. Composta por:

- **Back-end:** API HTTP/JSON que processa as requisições e mantém os dados em memória.
- **Front-end:** cliente de linha de comando (terminal) que consome a API.

As calorias são obtidas de **APIs externas**:

- [USDA FoodData Central](https://fdc.nal.usda.gov/api-guide) — calorias de alimentos.
- [API Ninjas — Calories Burned](https://api-ninjas.com/api/caloriesburned) — calorias gastas em atividades.

## Requisitos atendidos (spec)

- API em HTTP + JSON entre front-end e back-end.
- Transações como **hash-maps** armazenados em **lista** (`list`).
- Estado mantido em memória com **átomo** (`atom`).
- Estilo **funcional**: funções de ordem superior (`map`/`filter`/`reduce`/`some`),
  pureza (funções puras em `db.clj` separadas dos efeitos) e **recursão de cauda** (`recur`).
- **Nenhum** laço imperativo (`loop`/`while`/`for`/`doseq`/`dotimes`).

## Pré-requisitos

- [Leiningen](https://leiningen.org/) + Java 11+
- Chaves de API (ver abaixo)

## Configuração das chaves de API

As chaves são lidas de um arquivo `.env` na raiz do projeto (ou de variáveis de ambiente
do sistema, que têm prioridade). Copie o modelo e preencha:

```bash
cp .env.example .env
```

```ini
# .env
USDA_API_KEY=sua_chave        # https://fdc.nal.usda.gov/api-key-signup (ou DEMO_KEY)
API_NINJAS_KEY=sua_chave      # https://api-ninjas.com -> My Account
```

- `USDA_API_KEY`: se ausente, usa `DEMO_KEY` (funciona, porém com limite de requisições).
- `API_NINJAS_KEY`: obrigatória para registrar atividades físicas.

> O `.env` está no `.gitignore` e não é versionado.

## Como executar

Em um terminal, inicie o **back-end** (API):

```bash
lein run servidor          # porta 3000 (padrão); ou: lein run servidor 8080
```

Em outro terminal, inicie o **front-end** (cliente CLI):

```bash
lein run cliente           # conecta em http://localhost:3000
# ou apontando para outra URL/porta:
lein run cliente http://localhost:8080
```

O cliente exibe um menu com as operações; basta digitar o número e preencher os dados.

> Nomes de alimentos e atividades devem ser em **inglês** (ex.: `banana`, `chicken breast`,
> `skiing`), pois as APIs externas só aceitam esse idioma.

## Operações

| # | Operação                                   | Endpoint                       |
| - | ------------------------------------------ | ------------------------------ |
| 1 | Cadastrar dados pessoais                   | `POST /usuarios`               |
| 2 | Consultar dados pessoais                   | `GET /usuarios/:id`            |
| 3 | Registrar consumo de alimento (ganho)      | `POST /consumos`               |
| 4 | Registrar atividade física (perda)         | `POST /atividades`             |
| 5 | Consultar extrato de transações (período)  | `GET /extrato?usuario-id&inicio&fim` |
| 6 | Consultar saldo de calorias (período)      | `GET /saldo?usuario-id&inicio&fim`   |

`saldo = soma dos ganhos (alimentos) − soma das perdas (atividades)`.
Datas no formato ISO `YYYY-MM-DD`.

## API — exemplos (HTTP/JSON)

```bash
# 1. Cadastrar usuário
curl -X POST http://localhost:3000/usuarios -H 'Content-Type: application/json' \
  -d '{"nome":"Joao","altura":180,"peso":75,"idade":25,"sexo":"M"}'
# -> {"nome":"Joao","altura":180,"peso":75,"idade":25,"sexo":"M","id":1}

# 3. Registrar consumo de alimento (ganho)
curl -X POST http://localhost:3000/consumos -H 'Content-Type: application/json' \
  -d '{"usuario-id":1,"alimento":"banana","data":"2026-06-10","quantidade":150}'
# -> {"tipo":"ganho","categoria":"alimento","descricao":"BANANA","calorias":468.0,...}

# 4. Registrar atividade física (perda)
curl -X POST http://localhost:3000/atividades -H 'Content-Type: application/json' \
  -d '{"usuario-id":1,"atividade":"skiing","data":"2026-06-11","duracao":30}'
# -> {"tipo":"perda","categoria":"atividade","descricao":"Skiing...","calorias":224.0,...}

# 5. Extrato por período
curl "http://localhost:3000/extrato?usuario-id=1&inicio=2026-06-01&fim=2026-06-30"

# 6. Saldo por período
curl "http://localhost:3000/saldo?usuario-id=1&inicio=2026-06-01&fim=2026-06-30"
# -> {"periodo":{...},"ganho":798.0,"perda":224.0,"saldo":574.0}
```

## Arquitetura

Uma namespace por responsabilidade em `src/calcula_calorias/`:

```
client.clj  (CLI, HTTP) --JSON--> server.clj (rotas + middleware)
                                     -> handlers.clj (validacao, orquestracao, respostas)
                                          -> external.clj (USDA + API Ninjas)  [busca calorias]
                                          -> db.clj       (atom em memoria)     [persiste transacao]
config.clj  (carrega o .env)
```

- **db.clj** — funções **puras** `(estado -> novo-estado)` e wrappers `!` que aplicam ao
  átomo. Transações = hash-maps numa `list`. Filtro de período por comparação de datas ISO.
- **external.clj** — integrações externas. Energia da USDA é por 100 g e é escalada pela
  quantidade; API Ninjas recebe peso em libras (convertido de kg) e duração em minutos.
- **handlers.clj** — valida campos, chama `external` + `db`, devolve mapas `{:status :body}`.
- **server.clj** — rotas Compojure + middleware JSON e de parâmetros.
- **client.clj** — front-end CLI; menu por recursão de cauda; formata a saída.
- **config.clj** — leitor de `.env` (sem dependências externas).

## Testes

```bash
lein test                    # todos (testam a lógica pura de db.clj, sem rede)
lein check                   # compila todas as namespaces
```

## License

Copyright © 2026

Distribuído sob os termos da Eclipse Public License 2.0.
