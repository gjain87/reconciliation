package com.store.reconciliation.Utils;

import com.univocity.parsers.common.processor.RowListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CsvParserUtil {
    public record OrderRow(
            String orderId,
            String customerEmail,
            String currency,
            BigDecimal grossAmount,
            BigDecimal discount,
            BigDecimal netAmount,
            String status
    ) {
        public String getCleanId() {
            return orderId == null ? "" : orderId.trim().toUpperCase(Locale.ROOT);
        }
    }

    public record PaymentRow(
            String transactionRef,
            String orderReference,
            String currency,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal netSettled,
            String type,
            String status
    ) {
        public String getCleanOrderRef() {
            return orderReference == null ? "" : orderReference.trim().toUpperCase(Locale.ROOT);
        }
    }

    public static List<OrderRow> parseOrders(MultipartFile file) throws Exception {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setHeaderExtractionEnabled(true);
        settings.setNullValue("");
        settings.setEmptyValue("");

        RowListProcessor rowProcessor = new RowListProcessor();
        settings.setProcessor(rowProcessor);

        CsvParser parser = new CsvParser(settings);
        parser.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));

        String[] headers = rowProcessor.getHeaders();
        Map<String, Integer> headerMap = buildHeaderMap(headers);
        List<OrderRow> orders = new ArrayList<>();

        for (String[] row : rowProcessor.getRows()) {
            if (row.length == 0 || row[0] == null || row[0].isBlank()) continue;
            orders.add(new OrderRow(
                    getVal(row, headerMap, "order_id"),
                    getVal(row, headerMap, "customer_email"),
                    getVal(row, headerMap, "currency"),
                    parseDecimal(getVal(row, headerMap, "gross_amount")),
                    parseDecimal(getVal(row, headerMap, "discount")),
                    parseDecimal(getVal(row, headerMap, "net_amount")),
                    getVal(row, headerMap, "status").toLowerCase(Locale.ROOT)
            ));
        }
        return orders;
    }

    public static List<PaymentRow> parsePayments(MultipartFile file) throws Exception {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setHeaderExtractionEnabled(true);
        settings.setNullValue("");
        settings.setEmptyValue("");

        RowListProcessor rowProcessor = new RowListProcessor();
        settings.setProcessor(rowProcessor);

        CsvParser parser = new CsvParser(settings);
        parser.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));

        Map<String, Integer> headerMap = buildHeaderMap(rowProcessor.getHeaders());
        List<PaymentRow> payments = new ArrayList<>();

        for (String[] row : rowProcessor.getRows()) {
            if (row.length == 0 || row[0] == null || row[0].isBlank()) continue;
            payments.add(new PaymentRow(
                    getVal(row, headerMap, "transaction_ref"),
                    getVal(row, headerMap, "order_reference"),
                    getVal(row, headerMap, "currency"),
                    parseDecimal(getVal(row, headerMap, "amount")),
                    parseDecimal(getVal(row, headerMap, "fee")),
                    parseDecimal(getVal(row, headerMap, "net_settled")),
                    getVal(row, headerMap, "type").toLowerCase(Locale.ROOT),
                    getVal(row, headerMap, "status").toLowerCase(Locale.ROOT)
            ));
        }
        return payments;
    }

    private static Map<String, Integer> buildHeaderMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        if (headers == null) return map;
        for (int i = 0; i < headers.length; i++) {
            if (headers[i] != null) {
                map.put(headers[i].trim().toLowerCase(Locale.ROOT), i);
            }
        }
        return map;
    }

    private static String getVal(String[] row, Map<String, Integer> map, String colName) {
        Integer idx = map.get(colName);
        if (idx == null || idx >= row.length || row[idx] == null) return "";
        return row[idx].trim();
    }

    private static BigDecimal parseDecimal(String val) {
        if (val == null || val.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
