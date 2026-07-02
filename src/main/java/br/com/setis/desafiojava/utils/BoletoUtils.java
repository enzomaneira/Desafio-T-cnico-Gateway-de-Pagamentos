package br.com.setis.desafiojava.utils;

import br.com.setis.desafiojava.domain.entity.Provedor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class BoletoUtils {

  private static final LocalDate DATA_BASE_FATOR = LocalDate.of(1997, 10, 7);

  public static String gerarCodigoBarras(
      Provedor provedor, BigDecimal valor, LocalDateTime dataVencimento) {
    StringBuilder codigo = new StringBuilder();

    codigo.append(obterCodigoBanco(provedor));

    codigo.append("9");

    codigo.append(calcularFatorVencimento(dataVencimento));

    codigo.append(formatarValor(valor));

    codigo.append(gerarCampoLivreSimulado());

    String dv = calcularDigitoVerificadorGeral(codigo.toString());

    codigo.insert(4, dv);

    return codigo.toString();
  }

  private static String obterCodigoBanco(Provedor provedor) {
    return switch (provedor) {
      case BRADESCO -> "237";
      case ITAU -> "341";
      default -> throw new IllegalArgumentException(
          "O provedor " + provedor + " não suporta geração de boletos.");
    };
  }

  private static String calcularFatorVencimento(LocalDateTime vencimento) {
    if (vencimento == null) return "0000";
    long dias = ChronoUnit.DAYS.between(DATA_BASE_FATOR, vencimento.toLocalDate());

    String fator = String.valueOf(dias);
    if (fator.length() > 4) {
      fator = fator.substring(fator.length() - 4);
    }
    return String.format("%04d", Integer.parseInt(fator));
  }

  private static String formatarValor(BigDecimal valor) {
    if (valor == null) return "0000000000";
    String valorStr =
        valor.setScale(2, java.math.RoundingMode.HALF_EVEN).toString().replace(".", "");
    return String.format("%010d", Long.parseLong(valorStr));
  }

  private static String gerarCampoLivreSimulado() {
    StringBuilder sb = new StringBuilder();
    Random random = new Random();
    for (int i = 0; i < 25; i++) {
      sb.append(random.nextInt(10));
    }
    return sb.toString();
  }

  private static String calcularDigitoVerificadorGeral(String codigoSemDv) {
    int soma = 0;
    int peso = 2;

    for (int i = codigoSemDv.length() - 1; i >= 0; i--) {
      int digito = Character.getNumericValue(codigoSemDv.charAt(i));
      soma += digito * peso;
      peso++;
      if (peso > 9) peso = 2;
    }

    int resto = soma % 11;
    int dv = 11 - resto;

    if (dv == 10 || dv == 11) {
      return "1";
    }

    return String.valueOf(dv);
  }
}
