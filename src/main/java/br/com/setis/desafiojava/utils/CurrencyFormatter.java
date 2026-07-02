package br.com.setis.desafiojava.utils;

import java.util.Locale;
import java.util.Map;
import javax.money.MonetaryAmount;
import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryFormats;
import org.javamoney.moneta.format.CurrencyStyle;
import org.springframework.stereotype.Component;

@Component
public class CurrencyFormatter {
  private static final Map<String, Locale> CURRENCY_LOCALES =
      Map.of("BRL", Locale.of("pt", "BR"), "USD", Locale.US);

  public static String format(MonetaryAmount amount) {
    if (amount == null) return null;

    String currencyCode = amount.getCurrency().getCurrencyCode();
    Locale locale = CURRENCY_LOCALES.getOrDefault(currencyCode, Locale.getDefault());

    return MonetaryFormats.getAmountFormat(
            AmountFormatQueryBuilder.of(locale).set(CurrencyStyle.SYMBOL).build())
        .format(amount);
  }
}
