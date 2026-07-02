package br.com.setis.desafiojava.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CnpjUtilsTest {

  @Test
  @DisplayName("Deve limpar CNPJ removendo formatação e caracteres especiais")
  void deveLimparCnpjCorretamente() {
    String cnpjFormatado = "12.345.678/0001-99";
    String esperado = "12345678000199";

    String resultado = CnpjUtils.limpar(cnpjFormatado);

    assertEquals(esperado, resultado);
  }

  @Test
  @DisplayName("Deve retornar null quando entrada for null no limpar")
  void deveRetornarNullSeEntradaNull() {
    assertNull(CnpjUtils.limpar(null));
  }

  @Test
  @DisplayName("Deve transformar letras em maiúsculas ao limpar")
  void deveTransformarMaiusculas() {
    String cnpjAlfanumerico = "12.abc.345/0001-99";
    String esperado = "12ABC345000199";

    String resultado = CnpjUtils.limpar(cnpjAlfanumerico);

    assertEquals(esperado, resultado);
  }

  @Test
  @DisplayName("Deve retornar true para um CNPJ válido e conhecido")
  void deveValidarCnpjValido() {
    String cnpjValido = "00.000.000/0001-91";

    assertTrue(CnpjUtils.validar(cnpjValido));
  }

  @Test
  @DisplayName("Deve retornar false para CNPJ com dígito verificador errado")
  void deveInvalidarCnpjErrado() {
    String cnpjInvalido = "12345678000190";

    assertFalse(CnpjUtils.validar(cnpjInvalido));
  }

  @Test
  @DisplayName("Deve retornar false para CNPJ com tamanho incorreto")
  void deveInvalidarTamanhoIncorreto() {
    String curto = "123";
    String longo = "1234567890123456789";

    assertFalse(CnpjUtils.validar(curto));
    assertFalse(CnpjUtils.validar(longo));
  }

  @Test
  @DisplayName("Deve retornar false para CNPJ com todos dígitos iguais")
  void deveInvalidarDigitosIguais() {
    assertFalse(CnpjUtils.validar("00000000000000"));
    assertFalse(CnpjUtils.validar("11111111111111"));
  }

  @Test
  @DisplayName("Deve retornar false se entrada for null ou vazia")
  void deveInvalidarNullOuVazio() {
    assertFalse(CnpjUtils.validar(null));
    assertFalse(CnpjUtils.validar(""));
  }
}
