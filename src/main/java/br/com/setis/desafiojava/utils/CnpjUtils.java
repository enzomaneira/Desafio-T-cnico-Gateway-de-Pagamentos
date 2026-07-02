package br.com.setis.desafiojava.utils;

import org.springframework.stereotype.Component;

@Component
public class CnpjUtils {

  private static final int[] PESO_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
  private static final int[] PESO_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

  public static String limpar(String cnpj) {
    if (cnpj == null) return null;
    return cnpj.toUpperCase().replaceAll("[^A-Z0-9]", "");
  }

  public static boolean validar(String cnpj) {
    String limpo = limpar(cnpj);

    if (limpo == null || limpo.length() != 14) return false;

    if (limpo.matches("^(.)\\1*$")) return false;

    try {
      char dig13 = calcularDigito(limpo.substring(0, 12), PESO_1);
      char dig14 = calcularDigito(limpo.substring(0, 13), PESO_2);

      return dig13 == limpo.charAt(12) && dig14 == limpo.charAt(13);
    } catch (Exception e) {
      return false;
    }
  }

  private static char calcularDigito(String str, int[] peso) {
    int soma = 0;
    for (int i = str.length() - 1; i >= 0; i--) {
      int valorChar = converterCharParaInt(str.charAt(i));
      soma += valorChar * peso[peso.length - str.length() + i];
    }

    int resto = soma % 11;
    return resto < 2 ? '0' : (char) ((11 - resto) + 48);
  }

  private static int converterCharParaInt(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    } else {
      return c - 'A' + 10;
    }
  }
}
