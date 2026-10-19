package com.example.billing.service;

import com.example.billing.dto.BillRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Prices are treated as GST-exclusive. GST is split equally into CGST + SGST. */
public final class BillCalculator {

    public record Line(String name, BigDecimal qty, BigDecimal price, BigDecimal amount) {}

    public record Totals(List<Line> lines, BigDecimal subtotal, BigDecimal discount, BigDecimal taxable,
                         BigDecimal gstPercent, BigDecimal cgst, BigDecimal sgst, BigDecimal gstTotal,
                         BigDecimal roundOff, BigDecimal grandTotal) {}

    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private BillCalculator() {}

    public static Totals compute(BillRequest r) {
        List<Line> lines = r.items().stream()
                .map(i -> new Line(i.name(), i.qty(), i.price(), i.qty().multiply(i.price()).setScale(2, RM)))
                .toList();

        BigDecimal subtotal = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO.setScale(2);
        var d = r.discount();
        if (d != null && d.value() != null && d.value().signum() > 0) {
            discount = "PERCENT".equalsIgnoreCase(d.type())
                    ? subtotal.multiply(d.value()).divide(BigDecimal.valueOf(100), 2, RM)
                    : d.value().setScale(2, RM);
            discount = discount.min(subtotal);
        }

        BigDecimal taxable = subtotal.subtract(discount);
        BigDecimal gstPercent = r.gstPercent() == null ? BigDecimal.ZERO : r.gstPercent();
        BigDecimal gstTotal = taxable.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2, RM);
        BigDecimal cgst = gstTotal.divide(BigDecimal.valueOf(2), 2, RM);
        BigDecimal sgst = gstTotal.subtract(cgst);

        BigDecimal raw = taxable.add(gstTotal);
        BigDecimal grand = raw.setScale(0, RM).setScale(2);
        BigDecimal roundOff = grand.subtract(raw);

        return new Totals(lines, subtotal, discount, taxable, gstPercent, cgst, sgst, gstTotal, roundOff, grand);
    }
}
