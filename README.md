# Especificação de Requisitos de Software (SRS): Gateway de Pagamentos

**Projeto:** Desafio Técnico Setis - Gateway de Pagamentos

**Versão:** 1.1.0

**Data:** 02/07/2026

**Responsável:** Thiago Yakushiji / Stéfano Giordano

---

## 1. Introdução

### 1.1 Propósito
Este documento descreve os requisitos funcionais e não funcionais para o desenvolvimento de uma API de Gateway de Pagamentos. O sistema visa processar transações financeiras, gerir estornos e simular a comunicação com adquirentes, servindo como plataforma de avaliação técnica.

### 1.2 Escopo
O sistema abrange o ciclo de vida transacional (Autorização e Estorno), gestão de lojistas, gestão de usuários e visualização de dados segregada por perfil de acesso. A autenticação é realizada via Keycloak.

### 1.3 Definições e Acrônimos
* **PSP:** Provedor de Serviços de Pagamento.
* **EC:** Estabelecimento Comercial (Lojista).
* **PCI-DSS:** Payment Card Industry Data Security Standard.
* **RBAC:** Role-Based Access Control.
* **IAM:** Identity and Access Management.
* **JSR:** Java Specification Request.

---

## 2. Descrição Geral

### 2.1 Stack Tecnológica
* **Runtime:** Java 21 LTS.
* **Framework:** Spring Boot 3.2+ (Web, Data JPA, Security, Validation).
* **Banco de Dados:** MySQL 8.0 (Schema gerenciado por Liquibase).
* **API Financeira:** API `Moneta` (JSR 354).
* **IAM:** Keycloak (OIDC/OAuth2).

### 2.2 Atores e Controle de Acesso (RBAC)
O sistema deve implementar autorização baseada nas *roles* fornecidas pelo token JWT do Keycloak, mapeadas para o `ENUM` "Cargo".

| Ator        | Role (Enum: Cargo) | Descrição e Privilégios |
|:------------| :--- | :--- |
| **Lojista** | `GERENTE` | Acesso de leitura e escrita restrito aos recursos do seu próprio `merchant_id`. |
| **Lojista** | `ANALISTA` | Acesso de leitura restrito aos recursos do seu próprio `merchant_id`. |
| **Suporte** | `SUPORTE` | Acesso de leitura global (todos os lojistas) para fins de auditoria e atendimento. |
| **Admin**   | `ADMINISTRADOR` | Acesso irrestrito (Leitura/Escrita) a todos os recursos e configurações do sistema. |

---

## 3. Requisitos Funcionais (RF)

### [RF01] Processamento de Transações (Autorização)
O sistema deve prover um endpoint responsável pela captura e processamento de vendas.

* **Identificador:** `POST /v1/transacoes`
* **Headers Obrigatórios:**
  * `Authorization`: Bearer Token (JWT).
* **Headers Opcionais:**
  * `X-On-Behalf-Of`: idLojista (UUID).
  * obs: Apenas `SUPORTE`/`ADMINISTRADOR` podem usar esse header.
* **Entradas (`CriarTransacaoRequest`):**
  * `valor` (String): Valor em formato de "minor units" (centavos), sem pontuação. Ex: "100" = `R$ 1,00`; "5050" = `R$ 50,50`.
  * `moeda` (String): Código ISO 4217 (ex: "BRL").
  * `metodo` (Enum - MetodoPagamento): `PIX`, `BOLETO`, `CARTAO_CREDITO`, `CARTAO_DEBITO`.
  * `dadosPagamento` (Objeto - DadosPagamento):
    * Cartão (DadosCartaoRequest)
    * Pix (DadosPixRequest)
    * Boleto (dadosBoletoRequest)
* **Regras de Negócio:**
  1. O sistema deve persistir os dados do cartão de forma **mascarada** (apenas os 4 últimos dígitos), descartando CVV e Validade após o processamento.
  2. O sistema deve converter a string numérica para Decimal, considerando sempre os dois últimos dígitos como casas decimais (Ex: Input "500" → `BigDecimal` 5.00).
* **Saída Esperada:** `201 Created` contendo o UUID da transação, status processado e dados de pagamento sanitizados.

### [RF02] Simulação de Adquirente (PSP Interno)
O sistema deve conter um serviço interno que determina o resultado da transação com base nos centavos do valor.

* **Lógica de Decisão:**
  * Final `,00` → Status: `CONFIRMADA` (HTTP 200).
  * Final `,01` → Status: `NEGADA` (Motivo: Saldo Insuficiente).
  * Final `,02` → Status: `NEGADA` (Motivo: Bloqueio Antifraude).
  * Final `,03` → Status: `FALHA_COM_FORNECEDOR` (Erro 500).
  * Outros → Status: `CONFIRMADA`.

### [RF03] Gestão de Reembolsos
O sistema deve permitir o estorno total ou parcial de transações.

* **Identificador:** `POST /v1/transacoes/{id}/void?amount={valor}`
* **Headers Obrigatórios:** `Authorization`
* **Headers Opcionais:**
  * `X-On-Behalf-Of`: idLojista (UUID).
  * obs: Apenas `SUPORTE`/`ADMINISTRADOR` podem usar esse header.
* **Entradas:**
  * `amount` (Query Param, Opcional): Valor do estorno. Se omitido, assume o saldo restante total da transação.
* **Regras de Negócio:**
  1.  Todo o reembolso deve ter a inequação abaixo como verdadeira: $\sum (\text{Reembolsos Anteriores}) + \text{Reembolso Atual} \leq \text{Valor Original}$.
  2.  O sistema deve implementar **Lock** (Otimista ou Pessimista) para evitar *double refund* em requisições concorrentes.
  3.  Um reembolso deve ser associado a uma única transação (Relação N:1).
* **Saída Esperada:** `201 Created` com status `CONCLUIDO`

O sistema também deve permitir a listagem dos estornos realizados da transação
* **Identificador:** `GET /v1/transacoes/{id}/estornos`
* **Headers Obrigatórios:** `Authorization`
* **Headers Opcionais:**
  * `X-On-Behalf-Of`: idLojista (UUID).
  * obs: Apenas `SUPORTE`/`ADMINISTRADOR` podem usar esse header.

### [RF04] Consulta de Histórico
O sistema deve permitir a listagem de transações com filtros.

* **Identificador:** `GET /v1/transacoes`
* **Filtros (Query Params):** `dataInicio`, `dataFim`, `status`, `metodo`
* **Restrição de Segurança:**
  * `GERENTE`/`ANALISTA`: O sistema deve retornar apenas as transações vinculadas ao ID do lojista presente no Token JWT.
  * `SUPORTE`/`ADMINISTRADOR`: Podem filtrar por qualquer `lojistaId` usando o header X-On-Behalf-Of, passando o ID do Lojista.

### [RF05] Detalhe da Transação
O sistema deve exibir a visualização detalhada de uma operação.

* **Identificador:** `GET /v1/transacoes/{txId}`
* **Saída Esperada:**
  * DTO - TransacaoResponse

### [RF06] Gestão de Lojistas (CRUD)
O sistema deve permitir o gerenciamento das entidades de Estabelecimento Comercial.

* **Endpoints:**
  * `POST /v1/lojistas` - Cadastro de novo EC.
    * **Regras:** Criar usuário `GERENTE` inicial.
  * `GET /v1/lojistas/{id}` - Visualizar dados cadastrais.
  * `PUT /v1/lojistas/{id}` -  Remover acesso (Inativar) ou atualizar dados (Email, Configurações).
* **Restrições:** Apenas perfil `ADMINISTRADOR` pode criar ou editar lojistas. `GERENTE` não pode editar informações do próprio EC.

### [RF07] Gestão de usuários (CRUD)
O sistema deve permitir a gerência de usuários vinculados aos lojistas.

* **Endpoints:**
  * `POST /v1/lojistas/{id}/usuarios` - Adicionar novo usuário a um lojista.
  * `GET /v1/lojistas/{id}/usuarios` - Listar usuários do lojista.
  * `PUT /v1/lojistas/{id}/usuarios/{userId}` - Remover acesso (Inativar) ou edição de dados cadastrais.
* **Regras:** Integração com base local e Keycloak (criação do usuário no provedor de identidade).

---

## 4. Requisitos não funcionais (RNF)

* **[RNF01] Tratamento de Erros:** A API deve retornar erros estruturados. Stack traces e erros de SQL nunca devem ser expostos.
* **[RNF02] Padrões de API:** Aderência estrita aos verbos HTTP e Status Codes, API RESTful nível de maturidade 2.

---

## 5. Dicionário de dados (Domínio)

### 5.1 Cargo (Role)
```java
public enum Cargo {
  GERENTE,        // Acesso ao próprio EC (RW)
  ANALISTA,       // Acesso ao próprio EC (RO)
  SUPORTE,        // Acesso global RO para ECs e RW para Transações
  ADMINISTRADOR;   // Acesso global RW

  public static final Set<Cargo> CARGOS_PERMITIDOS_GERENTE = Set.of(GERENTE, ANALISTA); //Cargos permitidos para criar usuários quando gerente
  public static final Set<Cargo> CARGOS_PERMITIDOS_ON_BEHALF = Set.of(ADMINISTRADOR, SUPORTE); // Cargos permitidos para usar o header X-On-Behalf-Of
}
```
### 5.2 StatusTransacao
```java
public enum StatusTransacao {
  TRANSACAO_INICIADA,
  AGUARDANDO_PAGAMENTO,
  CONFIRMADA,
  FALHA_COM_FORNECEDOR,
  EXPIRADO,
  CANCELADA,
  NEGADA,
  REEMBOLSADA,
  PARCIALMENTE_REEMBOLSADA;

  public static final Set<StatusTransacao> PERMITEM_REEMBOLSO =
          Set.of(CONFIRMADA, PARCIALMENTE_REEMBOLSADA);
}
```

### 5.3 MetodoPagamento

```java

public enum MetodoPagamento {
PIX,
BOLETO,
CARTAO_CREDITO,
CARTAO_DEBITO
}

```

### 5.4 StatusReembolso

```java

public enum StatusReembolso {
PENDENTE,
CONCLUIDO,
FALHA_COM_FORNECEDOR
}
```

### 5.5 Provedores

```java
public enum Provedor {
  C6BANK,
  CIELO,
  ITAU,
  BRADESCO,
  SICOOB,
  GETNET,
  REDE,
  STONE,
  SICREDI;

  public static final Set<Provedor> ACEITAM_PIX =
      Set.of(C6BANK, CIELO, ITAU, SICOOB, SICREDI, BRADESCO);
  public static final Set<Provedor> ACEITAM_BOLETO = Set.of(BRADESCO, ITAU);
  public static final Set<Provedor> ACEITAM_CARTAO = Set.of(GETNET, REDE, STONE, CIELO);

  public boolean suporta(MetodoPagamento metodo) {
    if (metodo == null) return false;

    return switch (metodo) {
      case PIX -> ACEITAM_PIX.contains(this);
      case BOLETO -> ACEITAM_BOLETO.contains(this);
      case CARTAO_CREDITO, CARTAO_DEBITO -> ACEITAM_CARTAO.contains(this);
    };
  }
}
```
