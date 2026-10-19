package com.example.billing.service;

import com.example.billing.dto.BillRequest;
import com.example.billing.service.BillCalculator.Line;
import com.example.billing.service.BillCalculator.Totals;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.DottedLineSeparator;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Builds the two PDFs in memory. Nothing is written to disk or a database.
 * Note: the built-in PDF fonts have no rupee glyph, so "Rs." is used instead of the symbol.
 */
@Service
public class PdfService {

    private static final Color BRAND = new Color(0x4F, 0x46, 0xE5);
    private static final Color SOFT = new Color(0xEE, 0xF0, 0xFF);
    private static final Color LINE = new Color(0xD1, 0xD5, 0xDB);
    private static final Color GREY = new Color(0x55, 0x55, 0x55);

    // ------------------------------------------------------------------ A4

    public byte[] a4(BillRequest r, Totals t, String invoiceNo, String date) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            var s = r.shop();
            doc.add(para(s.name().toUpperCase(), f(20, Font.BOLD, BRAND), Element.ALIGN_CENTER));
            doc.add(para(s.address(), f(10, Font.NORMAL, GREY), Element.ALIGN_CENTER));
            doc.add(para("Ph: " + s.phones(), f(10, Font.NORMAL, GREY), Element.ALIGN_CENTER));
            Paragraph gstin = para("GSTIN: " + s.gstin(), f(10, Font.BOLD, Color.BLACK), Element.ALIGN_CENTER);
            gstin.setSpacingAfter(6);
            doc.add(gstin);
            doc.add(new Paragraph(new Chunk(new LineSeparator(1.2f, 100f, BRAND, Element.ALIGN_CENTER, -2f))));

            Paragraph title = para("TAX INVOICE", f(13, Font.BOLD, Color.BLACK), Element.ALIGN_CENTER);
            title.setSpacingBefore(8);
            title.setSpacingAfter(8);
            doc.add(title);

            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            meta.addCell(cell("Invoice No: " + invoiceNo, f(10, Font.BOLD, Color.BLACK), Element.ALIGN_LEFT, null, false, 2));
            meta.addCell(cell("Date: " + date, f(10, Font.BOLD, Color.BLACK), Element.ALIGN_RIGHT, null, false, 2));
            doc.add(meta);

            var c = r.customer();
            PdfPTable cust = new PdfPTable(1);
            cust.setWidthPercentage(100);
            cust.setSpacingBefore(6);
            Phrase cp = new Phrase();
            cp.add(new Chunk("BILLED TO\n", f(8, Font.BOLD, BRAND)));
            cp.add(new Chunk(c.name() + "\n", f(12, Font.BOLD, Color.BLACK)));
            cp.add(new Chunk("Mobile: " + c.mobile(), f(10, Font.NORMAL, Color.BLACK)));
            if (c.address() != null && !c.address().isBlank()) {
                cp.add(new Chunk("\n" + c.address(), f(10, Font.NORMAL, GREY)));
            }
            PdfPCell cc = new PdfPCell(cp);
            cc.setPadding(8);
            cc.setBorderColor(LINE);
            cc.setBackgroundColor(SOFT);
            cust.addCell(cc);
            doc.add(cust);

            PdfPTable items = new PdfPTable(5);
            items.setWidthPercentage(100);
            items.setWidths(new float[]{6, 44, 12, 18, 20});
            items.setSpacingBefore(14);
            items.setHeaderRows(1);
            Font hf = f(10, Font.BOLD, Color.WHITE);
            items.addCell(cell("#", hf, Element.ALIGN_CENTER, BRAND, false, 6));
            items.addCell(cell("Description", hf, Element.ALIGN_LEFT, BRAND, false, 6));
            items.addCell(cell("Qty", hf, Element.ALIGN_RIGHT, BRAND, false, 6));
            items.addCell(cell("Rate (Rs.)", hf, Element.ALIGN_RIGHT, BRAND, false, 6));
            items.addCell(cell("Amount (Rs.)", hf, Element.ALIGN_RIGHT, BRAND, false, 6));
            Font nf = f(10, Font.NORMAL, Color.BLACK);
            int idx = 1;
            for (Line l : t.lines()) {
                items.addCell(cell(String.valueOf(idx++), nf, Element.ALIGN_CENTER, null, true, 6));
                items.addCell(cell(l.name(), nf, Element.ALIGN_LEFT, null, true, 6));
                items.addCell(cell(qty(l.qty()), nf, Element.ALIGN_RIGHT, null, true, 6));
                items.addCell(cell(m(l.price()), nf, Element.ALIGN_RIGHT, null, true, 6));
                items.addCell(cell(m(l.amount()), nf, Element.ALIGN_RIGHT, null, true, 6));
            }
            doc.add(items);

            PdfPTable tot = new PdfPTable(2);
            tot.setWidthPercentage(45);
            tot.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tot.setSpacingBefore(10);
            tot.setWidths(new float[]{55, 45});
            totalsRows(tot, t, f(10, Font.NORMAL, Color.BLACK), 5);
            Font gf = f(12, Font.BOLD, Color.WHITE);
            tot.addCell(cell("GRAND TOTAL", gf, Element.ALIGN_LEFT, BRAND, false, 7));
            tot.addCell(cell("Rs. " + m(t.grandTotal()), gf, Element.ALIGN_RIGHT, BRAND, false, 7));
            doc.add(tot);

            Paragraph foot = para("Thank you for your business!", f(11, Font.BOLD, BRAND), Element.ALIGN_CENTER);
            foot.setSpacingBefore(30);
            doc.add(foot);
            doc.add(para("This is a computer generated invoice.", f(8, Font.NORMAL, GREY), Element.ALIGN_CENTER));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate A4 PDF", e);
        }
    }

    // ----------------------------------------------------------------- POS

    /** Thermal-receipt PDF: fixed width (80mm / 58mm), height fitted to the content. */
    public byte[] pos(BillRequest r, Totals t, String invoiceNo, String date, int widthMm) {
        float width = widthMm / 25.4f * 72f;
        float[] used = new float[1];
        renderPos(r, t, invoiceNo, date, width, 3000f, used);          // pass 1: measure
        return renderPos(r, t, invoiceNo, date, width, Math.max(used[0], 150f), null); // pass 2: final
    }

    private byte[] renderPos(BillRequest r, Totals t, String invoiceNo, String date,
                             float width, float height, float[] usedOut) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(new Rectangle(width, height), 6, 6, 8, 8);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            Font b = f(8, Font.BOLD, Color.BLACK);
            Font n = f(7.5f, Font.NORMAL, Color.BLACK);

            var s = r.shop();
            doc.add(para(s.name().toUpperCase(), f(11, Font.BOLD, Color.BLACK), Element.ALIGN_CENTER));
            doc.add(para(s.address(), n, Element.ALIGN_CENTER));
            doc.add(para("Ph: " + s.phones(), n, Element.ALIGN_CENTER));
            doc.add(para("GSTIN: " + s.gstin(), b, Element.ALIGN_CENTER));
            doc.add(dash());

            doc.add(para("Inv: " + invoiceNo, n, Element.ALIGN_LEFT));
            doc.add(para("Date: " + date, n, Element.ALIGN_LEFT));
            var c = r.customer();
            doc.add(para("Customer: " + c.name(), n, Element.ALIGN_LEFT));
            doc.add(para("Mobile: " + c.mobile(), n, Element.ALIGN_LEFT));
            if (c.address() != null && !c.address().isBlank()) {
                doc.add(para("Addr: " + c.address(), n, Element.ALIGN_LEFT));
            }
            doc.add(dash());

            PdfPTable items = new PdfPTable(2);
            items.setWidthPercentage(100);
            items.setWidths(new float[]{62, 38});
            items.addCell(cell("ITEM", b, Element.ALIGN_LEFT, null, false, 1));
            items.addCell(cell("AMOUNT", b, Element.ALIGN_RIGHT, null, false, 1));
            for (Line l : t.lines()) {
                PdfPCell name = cell(l.name(), b, Element.ALIGN_LEFT, null, false, 1);
                name.setColspan(2);
                items.addCell(name);
                items.addCell(cell(qty(l.qty()) + " x " + m(l.price()), n, Element.ALIGN_LEFT, null, false, 1));
                items.addCell(cell(m(l.amount()), n, Element.ALIGN_RIGHT, null, false, 1));
            }
            doc.add(items);
            doc.add(dash());

            PdfPTable tot = new PdfPTable(2);
            tot.setWidthPercentage(100);
            tot.setWidths(new float[]{60, 40});
            totalsRows(tot, t, n, 1);
            doc.add(tot);
            doc.add(dash());

            PdfPTable grand = new PdfPTable(2);
            grand.setWidthPercentage(100);
            grand.setWidths(new float[]{45, 55});
            Font gf = f(10.5f, Font.BOLD, Color.BLACK);
            grand.addCell(cell("TOTAL", gf, Element.ALIGN_LEFT, null, false, 1));
            grand.addCell(cell("Rs. " + m(t.grandTotal()), gf, Element.ALIGN_RIGHT, null, false, 1));
            doc.add(grand);
            doc.add(dash());

            Paragraph thanks = para("Thank you! Visit again.", b, Element.ALIGN_CENTER);
            thanks.setSpacingBefore(2);
            doc.add(thanks);

            if (usedOut != null) {
                usedOut[0] = height - writer.getVerticalPosition(true) + 8f;
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate POS PDF", e);
        }
    }

    // ------------------------------------------------------------- helpers

    /** Subtotal / discount / GST / round-off rows shared by both layouts. */
    private void totalsRows(PdfPTable tb, Totals t, Font font, float pad) {
        tb.addCell(cell("Subtotal", font, Element.ALIGN_LEFT, null, false, pad));
        tb.addCell(cell(m(t.subtotal()), font, Element.ALIGN_RIGHT, null, false, pad));
        if (t.discount().signum() > 0) {
            tb.addCell(cell("Discount", font, Element.ALIGN_LEFT, null, false, pad));
            tb.addCell(cell("- " + m(t.discount()), font, Element.ALIGN_RIGHT, null, false, pad));
            tb.addCell(cell("Taxable Amt", font, Element.ALIGN_LEFT, null, false, pad));
            tb.addCell(cell(m(t.taxable()), font, Element.ALIGN_RIGHT, null, false, pad));
        }
        if (t.gstPercent().signum() > 0) {
            String half = t.gstPercent().divide(BigDecimal.valueOf(2)).stripTrailingZeros().toPlainString();
            tb.addCell(cell("CGST @ " + half + "%", font, Element.ALIGN_LEFT, null, false, pad));
            tb.addCell(cell(m(t.cgst()), font, Element.ALIGN_RIGHT, null, false, pad));
            tb.addCell(cell("SGST @ " + half + "%", font, Element.ALIGN_LEFT, null, false, pad));
            tb.addCell(cell(m(t.sgst()), font, Element.ALIGN_RIGHT, null, false, pad));
        }
        if (t.roundOff().signum() != 0) {
            tb.addCell(cell("Round off", font, Element.ALIGN_LEFT, null, false, pad));
            tb.addCell(cell(m(t.roundOff()), font, Element.ALIGN_RIGHT, null, false, pad));
        }
    }

    private static Font f(float size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private static Paragraph para(String text, Font font, int align) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(align);
        return p;
    }

    private static Paragraph dash() {
        Paragraph p = new Paragraph(new Chunk(new DottedLineSeparator()));
        p.setSpacingBefore(1);
        p.setSpacingAfter(1);
        return p;
    }

    private static PdfPCell cell(String text, Font font, int align, Color bg, boolean border, float pad) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(align);
        c.setPadding(pad);
        c.setBorder(border ? Rectangle.BOX : Rectangle.NO_BORDER);
        c.setBorderColor(LINE);
        if (bg != null) c.setBackgroundColor(bg);
        return c;
    }

    private static String m(BigDecimal v) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }

    private static String qty(BigDecimal q) {
        return q.stripTrailingZeros().toPlainString();
    }
}
