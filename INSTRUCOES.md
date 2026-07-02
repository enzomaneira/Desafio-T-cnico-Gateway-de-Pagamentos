# Desafio Técnico — Gateway de Pagamentos

Este repositório contém uma API de Gateway de Pagamentos em Spring Boot, construída a partir da especificação descrita no [README.md](README.md). Leia a especificação com atenção, ela é a fonte de verdade sobre o comportamento esperado do sistema.

O prazo de entrega está no e-mail que acompanha este desafio.

---

## 1. Seu Desafio

O projeto compila, mas a suíte de testes **não está passando** e existem pontos do código marcados com `todo` que precisam ser implementados.

Sua tarefa:

1. **Execute a suíte de testes** e analise as falhas.
2. **Corrija o código** até que **todos os testes fiquem verdes**.
3. **Implemente os trechos marcados com `todo`**, guiando-se pelos testes existentes e pela especificação do README.
4. Um dos testes está vazio, marcado com um comentário `implementar este teste` — **escreva você mesmo esse teste**, cobrindo a regra de negócio correspondente da especificação.

Dicas importantes:

- Nem toda falha de teste corresponde a um problema diferente — investigue a **causa raiz** antes de sair corrigindo sintomas.
- A correção certa fica no **código de produção**, não nos testes. Alterar asserções ou apagar testes para "fazer passar" invalida o desafio (a única exceção é o teste vazio que você deve implementar).
- O README descreve as regras de negócio. Quando um teste e o seu instinto discordarem, consulte a especificação.

## 2. Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| JDK | **21** | Obrigatório. Verifique com `java -version`. Versões anteriores falham com `release version 21 not supported`. |
| Docker + Docker Compose | Qualquer recente | Opcional — necessário apenas para rodar a aplicação completa (MySQL + Keycloak). **Os testes não precisam de Docker.** |
| Maven | — | Não precisa instalar. Use o Maven Wrapper incluso (`mvnw` / `mvnw.cmd`). |

## 3. Como executar

### Testes (o essencial do desafio)

```bash
# Linux / macOS
./mvnw test

# Windows
mvnw.cmd test
```

Observação: o build executa o Spotless (google-java-format) automaticamente na fase `validate` e **formata o código por você**. Não estranhe se arquivos forem reformatados ao rodar o Maven.

### Aplicação completa (opcional)

Útil para explorar a API manualmente, mas não é obrigatório para concluir o desafio.

```bash
# Sobe MySQL (porta 3306) e Keycloak (porta 8180)
docker compose up -d

# Sobe a API
./mvnw spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- O schema do banco é gerenciado pelo Liquibase na subida da aplicação.

## 4. Entrega

1. Trabalhe em uma branch separada (ex.: `desafio/seu-nome`).
2. Faça **commits pequenos e com mensagens claras** — o histórico faz parte da avaliação.
3. Ao final, faça merge da sua branch para a `main` contendo:
   - Num arquivo .md o resumo do que foi corrigido/implementado e **por quê** (a causa raiz de cada problema);
   - Possíveis premissas que você assumiu em pontos ambíguos;
   - (Diferencial) melhorias que você faria com mais tempo.

Como o desafio é assíncrono, dúvidas sobre ambiguidades não invalidarão sua entrega: **decida, documente a premissa no PR e siga em frente**.

## 5. O que será avaliado

| Critério | O que olhamos |
|---|---|
| Suíte verde | Todos os testes passando, sem testes alterados/ignorados |
| Diagnóstico | Correções que atacam a causa raiz, não o sintoma |
| Aderência à spec | Comportamento conforme o README (status HTTP, regras de negócio, RBAC) |
| Qualidade do teste escrito | Cenário correto, mocks bem usados, asserções relevantes |
| Comunicação | Mensagens de commit e descrição do PR claras e honestas |

## 6. O que evitar

- Alterar ou remover testes existentes (exceto o teste vazio a implementar);
- Adicionar dependências novas sem justificar no PR;
- Reformatações em massa não relacionadas (o Spotless já cuida da formatação);
- Commits gigantes do tipo "fix all".

Bom desafio!
