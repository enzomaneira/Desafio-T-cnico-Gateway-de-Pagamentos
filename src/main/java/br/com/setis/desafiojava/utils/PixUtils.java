package br.com.setis.desafiojava.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PixUtils {

  // IDs do Padrão EMV (BR Code)
  private static final String ID_PAYLOAD_FORMAT_INDICATOR = "00";
  private static final String ID_MERCHANT_ACCOUNT_INFORMATION = "26";
  private static final String ID_MERCHANT_CATEGORY_CODE = "52";
  private static final String ID_TRANSACTION_CURRENCY = "53";
  private static final String ID_TRANSACTION_AMOUNT = "54";
  private static final String ID_COUNTRY_CODE = "58";
  private static final String ID_MERCHANT_NAME = "59";
  private static final String ID_MERCHANT_CITY = "60";
  private static final String ID_ADDITIONAL_DATA_FIELD_TEMPLATE = "62";
  private static final String ID_CRC16 = "63";

  private static final String BRL_CURRENCY = "986";
  private static final String MCC_GENERICO = "0000";
  private static final String CODIGO_PAIS = "BR";
  private static final String CIDADE = "SAO PAULO";

  public static String gerarEmvCopiaCola(
      String chavePix, String nomeLojista, BigDecimal valor, String txId) {
    StringBuilder emv = new StringBuilder();

    // 00 - Payload Format
    append(emv, ID_PAYLOAD_FORMAT_INDICATOR, "01");

    // 26 - Merchant Account (GUI + Chave)
    String gui = "0014br.gov.bcb.pix";
    String key = "01" + String.format("%02d", chavePix.length()) + chavePix;
    append(emv, ID_MERCHANT_ACCOUNT_INFORMATION, gui + key);

    // 52 - MCC Genérico, não vamos olhar para essa info
    append(emv, ID_MERCHANT_CATEGORY_CODE, MCC_GENERICO);

    // 53 - Currency (986 = BRL)
    append(emv, ID_TRANSACTION_CURRENCY, BRL_CURRENCY);

    // 54 - Valor (Formato 0.00 sem separador de milhar)
    if (valor != null) {
      DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
      DecimalFormat df = new DecimalFormat("0.00", symbols);
      append(emv, ID_TRANSACTION_AMOUNT, df.format(valor));
    }

    // 58 - Country Code
    append(emv, ID_COUNTRY_CODE, CODIGO_PAIS);

    // 59 - Merchant Name (Max 25 chars)
    String nomeFormatado =
        nomeLojista == null ? "LOJISTA" : nomeLojista.toUpperCase().replaceAll("[^A-Z0-9 ]", "");
    if (nomeFormatado.length() > 25) nomeFormatado = nomeFormatado.substring(0, 25);
    append(emv, ID_MERCHANT_NAME, nomeFormatado);

    // 60 - City (constante São Paulo)
    append(emv, ID_MERCHANT_CITY, CIDADE);

    // 62 - TxID
    String txIdFormatado = txId == null ? "***" : txId;
    String subField = "05" + String.format("%02d", txIdFormatado.length()) + txIdFormatado;
    append(emv, ID_ADDITIONAL_DATA_FIELD_TEMPLATE, subField);

    // 63 - CRC16
    String payloadSemCrc = emv + ID_CRC16 + "04";
    String crc = calcularCRC16(payloadSemCrc);

    return payloadSemCrc + crc;
  }

  private static void append(StringBuilder sb, String id, String value) {
    sb.append(id);
    sb.append(String.format("%02d", value.length()));
    sb.append(value);
  }

  private static String calcularCRC16(String payload) {
    int crc = 0xFFFF;
    int polynomial = 0x1021;

    for (byte b : payload.getBytes()) {
      for (int i = 0; i < 8; i++) {
        boolean bit = ((b >> (7 - i) & 1) == 1);
        boolean c15 = ((crc >> 15 & 1) == 1);
        crc <<= 1;
        if (c15 ^ bit) crc ^= polynomial;
      }
    }
    crc &= 0xFFFF;
    return String.format("%04X", crc);
  }
}
