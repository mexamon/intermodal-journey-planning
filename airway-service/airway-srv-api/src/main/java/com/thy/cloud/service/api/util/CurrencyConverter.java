package com.thy.cloud.service.api.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Centralized currency converter with static exchange rates.
 * <p>
 * All rates are expressed as: 1 unit of SOURCE currency = X units of EUR (base).
 * Conversion: amount_in_target = amount_in_source * toEurRate(source) / toEurRate(target)
 * <p>
 * Example:
 *   CurrencyConverter.convert(225500, "TRY", "EUR")  → 5637  (₺2255.00 → €56.37)
 *   CurrencyConverter.convert(18500, "EUR", "TRY")   → 741481 (€185.00 → ₺7414.81)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CurrencyConverter {

    /**
     * Exchange rates: 1 unit of currency = X EUR.
     * Updated: 2026-03-08 (static snapshot — replace with live API later).
     */
    private static final Map<String, Double> TO_EUR_RATES = Map.ofEntries(
            Map.entry("EUR", 1.0),
            Map.entry("TRY", 0.025),     // 1 TRY = 0.025 EUR  (1 EUR ≈ 40 TRY)
            Map.entry("GBP", 1.17),      // 1 GBP = 1.17 EUR
            Map.entry("USD", 0.92),      // 1 USD = 0.92 EUR
            Map.entry("CHF", 1.06),      // 1 CHF = 1.06 EUR
            Map.entry("SEK", 0.087),     // 1 SEK = 0.087 EUR
            Map.entry("NOK", 0.085),     // 1 NOK = 0.085 EUR
            Map.entry("DKK", 0.134),     // 1 DKK = 0.134 EUR
            Map.entry("PLN", 0.232),     // 1 PLN = 0.232 EUR
            Map.entry("CZK", 0.040),     // 1 CZK = 0.040 EUR
            Map.entry("HUF", 0.0025),    // 1 HUF = 0.0025 EUR
            Map.entry("RUB", 0.0095),    // 1 RUB = 0.0095 EUR
            Map.entry("AED", 0.25),      // 1 AED = 0.25 EUR
            Map.entry("SAR", 0.245),     // 1 SAR = 0.245 EUR
            Map.entry("JPY", 0.0061),    // 1 JPY = 0.0061 EUR
            Map.entry("CNY", 0.127),     // 1 CNY = 0.127 EUR
            Map.entry("INR", 0.011),     // 1 INR = 0.011 EUR
            Map.entry("KRW", 0.00067),   // 1 KRW = 0.00067 EUR
            Map.entry("CAD", 0.67),      // 1 CAD = 0.67 EUR
            Map.entry("AUD", 0.60),      // 1 AUD = 0.60 EUR
            Map.entry("BRL", 0.155)      // 1 BRL = 0.155 EUR
    );

    /**
     * Convert an amount (in cents) from one currency to another.
     *
     * @param amountCents amount in smallest unit (cents/kuruş/pence)
     * @param fromCurrency ISO currency code of the source (e.g. "TRY")
     * @param toCurrency   ISO currency code of the target (e.g. "EUR")
     * @return converted amount in cents of the target currency
     * @throws IllegalArgumentException if either currency is unknown
     */
    public static int convert(int amountCents, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) return amountCents;
        if (fromCurrency.equals(toCurrency)) return amountCents;

        double fromRate = getRate(fromCurrency);
        double toRate = getRate(toCurrency);

        // source_cents → EUR_cents → target_cents
        return (int) Math.round(amountCents * fromRate / toRate);
    }

    /**
     * Convert an amount (in cents) to EUR.
     */
    public static int toEur(int amountCents, String fromCurrency) {
        return convert(amountCents, fromCurrency, "EUR");
    }

    /**
     * Convert an amount (in cents) from EUR to target currency.
     */
    public static int fromEur(int amountCents, String toCurrency) {
        return convert(amountCents, "EUR", toCurrency);
    }

    /**
     * Get the EUR exchange rate for a currency.
     * @return how many EUR 1 unit of the currency is worth
     */
    public static double getRate(String currency) {
        if (currency == null) return 1.0;
        Double rate = TO_EUR_RATES.get(currency.toUpperCase());
        if (rate == null) {
            throw new IllegalArgumentException("Unknown currency: " + currency +
                    ". Supported: " + TO_EUR_RATES.keySet());
        }
        return rate;
    }

    /**
     * Check if a currency is supported.
     */
    public static boolean isSupported(String currency) {
        return currency != null && TO_EUR_RATES.containsKey(currency.toUpperCase());
    }

    /**
     * Get the currency symbol for display.
     */
    public static String getSymbol(String currency) {
        if (currency == null) return "€";
        return switch (currency.toUpperCase()) {
            case "EUR" -> "€";
            case "TRY" -> "₺";
            case "GBP" -> "£";
            case "USD" -> "$";
            case "CHF" -> "CHF";
            case "JPY" -> "¥";
            case "CNY" -> "¥";
            case "KRW" -> "₩";
            case "INR" -> "₹";
            case "RUB" -> "₽";
            case "BRL" -> "R$";
            default -> currency;
        };
    }
}
