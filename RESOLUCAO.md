# Testes com Falha — Erros e Causas Raiz

## Erros encontrados

1. `TransacaoControllerTest.deveGerarTransacaoComSucesso` — Esperado 201, obtido 400
2. `TransacaoControllerTest.deveGerarTransacaoComSucessoComHeaderOnBehalf` — Esperado 201, obtido 400
3. `TransacaoControllerTest.deveIgnorarHeaderOnBehalfOfSeNaoForAdmin` — Esperado 201, obtido 400
4. `TransacaoControllerTest.deveRetornarErroDeNegocio` — Mock do service nunca é chamado
5. `TransacaoControllerTest.deveRetornarForbiddenSemRole` — Esperado 403, obtido 400
6. `TransacaoControllerTest.deveTratarFalhaComunicacao` — Esperado 500, obtido 400
7. `TransacaoControllerTest.deveTratarTransacaoRecusada` — Esperado 402, obtido 400
8. `LojistaServiceTest.deveCadastrarLojistaComSucesso` — Esperado true, obtido false
9. `ProcessadorDePagamentosTest.deveAprovarTransacao` — Esperado CONFIRMADA, obtido AGUARDANDO_PAGAMENTO
10. `ProcessadorPagamentoFactoryTest.deveRetornarEstrategiaCorreta` — Esperado processarBoleto, obtido processarPix
11. `ProcessadorAssincronoTest.deveIniciarProcessamentoBoleto` — Esperado AGUARDANDO_PAGAMENTO, obtido null

## Causas raiz

### 1, 2, 3, 4, 6, 7 — Testes de `TransacaoControllerTest` (6 testes)
**Causa:** O campo `chavePix` em `DadosPixRequest` está anotado com `@UUID`, mas o helper `criarRequestValido()` usado nos testes envia um CNPJ (`"77027447000189"`). Isso falha a validação Bean Validation e o Spring devolve 400 antes de a requisição chegar ao controller/service. Por isso o mock do `TransacaoService` nunca é invocado e todos os status esperados (201, 402, 500) são substituídos por 400.

### 5 — `deveRetornarForbiddenSemRole`
**Causa:** Mesmo bug do `chavePix` acima. No pipeline do Spring MVC, o `@Valid` do corpo da requisição é avaliado antes do `@PreAuthorize`, então o payload inválido gera 400 e mascara o 403 esperado do RBAC.

### 8 — `LojistaServiceTest.deveCadastrarLojistaComSucesso`
**Causa:** Teste não implementado — corpo contém apenas `Assertions.assertTrue(false)` com comentário `todo`.

### 9 — `ProcessadorDePagamentosTest.deveAprovarTransacao`
**Causa:** No branch `default` do switch em `ProcessadorDePagamentos.simularProcessamentoExterno`, o status é setado como `AGUARDANDO_PAGAMENTO`, mas deveria ser `CONFIRMADA` (fluxo representa aprovação síncrona do PSP).

### 10 — `ProcessadorPagamentoFactoryTest.deveRetornarEstrategiaCorreta`
**Causa:** Em `ProcessadorPagamentoFactory.get(MetodoPagamento metodo)`, a busca no mapa usa a constante fixa `PIX` (`estrategias.get(PIX)`) em vez do parâmetro recebido (`estrategias.get(metodo)`). Por isso qualquer método retorna sempre a estratégia de PIX.

### 11 — `ProcessadorAssincronoTest.deveIniciarProcessamentoBoleto`
**Causa:** `ProcessarBoleto.processar()` está com corpo vazio (`todo`), então nenhum campo da transação é preenchido e `status` permanece `null`.
