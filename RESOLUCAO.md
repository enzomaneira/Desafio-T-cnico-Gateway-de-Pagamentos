# Testes com Falha — Erros e Causas Raiz

## Erros encontrados

1. `TransacaoControllerTest.deveGerarTransacaoComSucesso` — Esperado 201, obtido 400
[ERROR] TransacaoControllerTest.deveGerarTransacaoComSucesso:86 Status expected:<201> but was:<400>
2. `TransacaoControllerTest.deveGerarTransacaoComSucessoComHeaderOnBehalf` — Esperado 201, obtido 400
[ERROR] TransacaoControllerTest.deveGerarTransacaoComSucessoComHeaderOnBehalf:117 Status expected:<201> but was:<400>
3. `TransacaoControllerTest.deveIgnorarHeaderOnBehalfOfSeNaoForAdmin` — Esperado 201, obtido 400
[ERROR] TransacaoControllerTest.deveIgnorarHeaderOnBehalfOfSeNaoForAdmin:147 Status expected:<201> but was:<400>
4. `TransacaoControllerTest.deveRetornarErroDeNegocio` — Mock do service nunca é chamado
[ERROR] TransacaoControllerTest.deveRetornarErroDeNegocio:193 
Wanted but not invoked:
br.com.setis.desafiojava.service.TransacaoService#0.criarTransacao(
    <any>,
    <any>,
    <any>
);
-> at br.com.setis.desafiojava.controller.TransacaoControllerTest.deveRetornarErroDeNegocio(TransacaoControllerTest.java:193)
Actually, there were zero interactions with this mock.
5. `TransacaoControllerTest.deveRetornarForbiddenSemRole` — Esperado 403, obtido 400
[ERROR] TransacaoControllerTest.deveRetornarForbiddenSemRole:208 Status expected:<403> but was:<400>
6. `TransacaoControllerTest.deveTratarFalhaComunicacao` — Esperado 500, obtido 400
[ERROR] TransacaoControllerTest.deveTratarFalhaComunicacao:694 Status expected:<500> but was:<400>
7. `TransacaoControllerTest.deveTratarTransacaoRecusada` — Esperado 402, obtido 400
[ERROR] TransacaoControllerTest.deveTratarTransacaoRecusada:660 Status expected:<402> but was:<400>
8. `LojistaServiceTest.deveCadastrarLojistaComSucesso` — Esperado true, obtido false
[ERROR] LojistaServiceTest.deveCadastrarLojistaComSucesso:43 expected: <true> but was: <false>
9. `ProcessadorDePagamentosTest.deveAprovarTransacao` — Esperado CONFIRMADA, obtido AGUARDANDO_PAGAMENTO
[ERROR] Tests run: 4, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.021 s <<< FAILURE! -- in br.com.setis.desafiojava.strategy.ProcessadorDePagamentosTest
[ERROR] br.com.setis.desafiojava.strategy.ProcessadorDePagamentosTest.deveAprovarTransacao -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <CONFIRMADA> but was: <AGUARDANDO_PAGAMENTO>
	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
	at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:177)
	at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1145)
	at br.com.setis.desafiojava.strategy.ProcessadorDePagamentosTest.deveAprovarTransacao(ProcessadorDePagamentosTest.java:85)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
10. `ProcessadorPagamentoFactoryTest.deveRetornarEstrategiaCorreta` — Esperado processarBoleto, obtido processarPix
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.233 s <<< FAILURE! -- in br.com.setis.desafiojava.strategy.ProcessadorPagamentoFactoryTest
[ERROR] br.com.setis.desafiojava.strategy.ProcessadorPagamentoFactoryTest.deveRetornarEstrategiaCorreta -- Time elapsed: 0.227 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <processarBoleto> but was: <processarPix>
	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
	at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
	at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:177)
	at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1145)
	at br.com.setis.desafiojava.strategy.ProcessadorPagamentoFactoryTest.deveRetornarEstrategiaCorreta(ProcessadorPagamentoFactoryTest.java:36)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
11. `ProcessadorAssincronoTest.deveIniciarProcessamentoBoleto` — Esperado AGUARDANDO_PAGAMENTO, obtido null
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.021 s <<< FAILURE! -- in br.com.setis.desafiojava.strategy.impl.ProcessadorAssincronoTest
[ERROR] br.com.setis.desafiojava.strategy.impl.ProcessadorAssincronoTest.deveIniciarProcessamentoBoleto -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <AGUARDANDO_PAGAMENTO> but was: <null>
	at br.com.setis.desafiojava.strategy.impl.ProcessadorAssincronoTest.deveIniciarProcessamentoBoleto(ProcessadorAssincronoTest.java:61)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

## Causas raiz

### 1, 2, 3, 4, 5, 6, 7 — Testes de `TransacaoControllerTest` (6 testes)
**Causa:** Rodei os 6 testes e todos falhavam com 400. Fui ver o TransacaoController e o GlobalExceptionHandler, o erro 400 acontece por erro de validação ou IllegalArgumentException. Fui no DTO DadosPixRequest e achei: o campo chavePix tem @UUID, o que invalida 4 das 5 possibilidades de formato da chave pix - https://www.baeldung.com/java-hibernate-uuid-primary-key
**Solução:** Pensei em apenas apagar o UUID, porém isso iria apenas enfraquecer a validação do corpo da requisição, então acho que vou fazer uma nova validação especial para os formatos de chavePix. 
Criei um arquivo em dto.validation chamado ChavePixValida


### 8 — `LojistaServiceTest.deveCadastrarLojistaComSucesso`
**Causa:**  `Assertions.assertTrue(false)` com comentário `todo`. Precisamos implementar. 
**Solução:** 
1. Mockar existePorEmail e existsByCnpj como false: as duas condições que se TRUE retornariam IllegalArgumentException. Ambas precisam ser FALSE. 
2. Mocka save com thenAnswer: como Lojista.builder...build() não tem id setado manualmente, o mock simplesmente devolve o mesmo objeto, o que permite inspecionar os campos depois. 
3. Verifica os campos do lojista retornado: confirma que cnpj e nomeFantasia foram propagados corretamente do request para a entidade



### 9 — `ProcessadorDePagamentosTest.deveAprovarTransacao`
**Causa:** Em `default` do switch em `ProcessadorDePagamentos.simularProcessamentoExterno`, o status é setado como `AGUARDANDO_PAGAMENTO`, mas deveria ser `CONFIRMADA`, como descrito no RF02. 
**Solução** na branch default do switch na classe processadorDePagamentos o status estava sendo setado para AGUARDANDO_PAGAMENTO quando na verdade deveria ser CONFIRMADA, o que contrariava a regra Outros -> Status: CONFIRMADA no RF02.

### 10 — `ProcessadorPagamentoFactoryTest.deveRetornarEstrategiaCorreta`
**Causa:** Em `ProcessadorPagamentoFactory.get(MetodoPagamento metodo)`, a busca no mapa usa uma constante fixa com o valor `PIX` em vez do parâmetro recebido (`estrategias.get(metodo)`). Por isso qualquer método retorna sempre a estratégia de PIX.
**Solução** Trocar o valor fixo PIX pelo retorno do da chamada estrategias.get(metodo)

### 11 — `ProcessadorAssincronoTest.deveIniciarProcessamentoBoleto`
**Causa:** `ProcessarBoleto.processar()` precisa ser implementado. 
**Solução** Implementação método processar() em ProcessarBoleto gerando o codigo de barras via BoletoUtils e setando os status no mesmo padrão do método ProcessarPix