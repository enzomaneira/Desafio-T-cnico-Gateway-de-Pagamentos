# Testes com Falha — Erros e Causas Raiz

## Erros encontrados

1. `TransacaoControllerTest.deveGerarTransacaoComSucesso` — Esperado 201, obtido 400
2. `TransacaoControllerTest.deveGerarTransacaoComSucessoComHeaderOnBehalf` — Esperado 201, obtido 400
3. `TransacaoControllerTest deveIgnorarHeaderOnBehalfOfSeNaoForAdmin` — Esperado 201, obtido 400
4. `TransacaoControllerTest.deveRetornarErroDeNegocio` — Mock do service nunca é chamado
5. `TransacaoControllerTest.deveRetornarForbiddenSemRole` — Esperado 403, obtido 400
6. `TransacaoControllerTest.deveTratarFalhaComunicacao` — Esperado 500, obtido 400
7. `TransacaoControllerTest.deveTratarTransacaoRecusada` — Esperado 402, obtido 400
8. `LojistaServiceTest.deveCadastrarLojistaComSucesso` — Esperado true, obtido false
9. `ProcessadorDePagamentosTest.deveAprovarTransacao` — Esperado CONFIRMADA, obtido AGUARDANDO_PAGAMENTO
10. `ProcessadorPagamentoFactoryTest.deveRetornarEstrategiaCorreta` — Esperado processarBoleto, obtido processarPix
11. `ProcessadorAssincronoTest.deveIniciarProcessamentoBoleto` — Esperado AGUARDANDO_PAGAMENTO, obtido null

## Causas raiz

### 1, 2, 3, 4, 5, 6, 7 — Testes de `TransacaoControllerTest` (6 testes)
**Causa:** Rodei os 6 testes e todos falhavam com 400. Fui no DTO DadosPixRequest e achei: o campo chavePix tem @UUID, o que invalida 4 das 5 possibilidades de formato da chave pix - https://www.baeldung.com/java-hibernate-uuid-primary-key
**Solução:** Pensei em apenas apagar o UUID, porém isso iria apenas enfraquecer a validação do corpo da requisição, então acho que vou fazer uma nova validação especial para os formatos de chavePix. 
Criei um arquivo em dto.validation chamado ChavePixValida


### 8 — `LojistaServiceTest.deveCadastrarLojistaComSucesso`
**Causa:**  `Assertions.assertTrue(false)` com comentário `todo`. Precisamos implementar. 
**Solução:** 
1. Mockar existePorEmail e existsByCnpj como false: as duas condições que se TRUE retornariam IllegalArgumentException. Ambas precisam ser FALSE. 
2. Mocka save com thenAnswer: como Lojista.builder...build() não tem id setado manualmente, o mock simplesmente devolve o mesmo objeto, o que permite inspecionar os campos depois. 
3. Verifica os campos do lojista retornado: confirma que cnpj e nomeFantasia foram propagados corretamente do request para a entidade
https://www.baeldung.com/mockito-behavior



### 9 — `ProcessadorDePagamentosTest.deveAprovarTransacao`
**Causa:** Em `default` do switch em `ProcessadorDePagamentos.simularProcessamentoExterno`, o status é setado como `AGUARDANDO_PAGAMENTO`, mas deveria ser `CONFIRMADA`, como descrito no RF02. 
**Solução** na branch default do switch na classe processadorDePagamentos o status estava sendo setado para AGUARDANDO_PAGAMENTO quando na verdade deveria ser CONFIRMADA, o que contrariava a regra Outros -> Status: CONFIRMADA no RF02.

### 10 — `ProcessadorPagamentoFactoryTest.deveRetornarEstrategiaCorreta`
**Causa:** Em `ProcessadorPagamentoFactory.get(MetodoPagamento metodo)`, a busca no mapa usa uma constante fixa com o valor `PIX` em vez do parâmetro recebido (`estrategias.get(metodo)`). Por isso qualquer método retorna sempre a estratégia de PIX.
**Solução** Trocar o valor fixo PIX pelo retorno do da chamada estrategias.get(metodo)

### 11 — `ProcessadorAssincronoTest.deveIniciarProcessamentoBoleto`
**Causa:** `ProcessarBoleto.processar()` precisa ser implementado. 
**Solução** Implementação método processar() em ProcessarBoleto gerando o codigo de barras via BoletoUtils e setando os status no mesmo padrão do método ProcessarPix


## Melhorias

### 1. Código duplicado entre `ProcessarPix` e `ProcessarBoleto`

Os dois métodos `simularPagamentoAssincrono` são quase iguais: mesmo `sleep(30)`, mesmo `try/catch`, mesma chamada a `simularProcessamentoExterno` + `save`.

Dava pra mover isso pra classe abstrata `ProcessadorDePagamentos`, deixando só o `TransacaoRepository`. Assim reduz duplicação e qualquer correção futura (como a do item de tratamento de exceção) é feita em um lugar só.

### 2. Falta teste para a anotação `@ChavePixValida`

Criei essa validação customizada pra resolver os testes 1 a 7, mas não existe um teste unitário só pra ela.

