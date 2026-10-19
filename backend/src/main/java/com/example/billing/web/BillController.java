package com.example.billing.web;

import com.example.billing.dto.BillRequest;
import com.example.billing.service.BillCalculator;
import com.example.billing.service.PdfService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final PdfService pdf;

    public BillController(PdfService pdf) {
        this.pdf = pdf;
    }

    @PostMapping("/a4")
    public ResponseEntity<byte[]> a4(@Valid @RequestBody BillRequest req) {
        var now = LocalDateTime.now();
        String inv = invoiceNo(req, now);
        var totals = BillCalculator.compute(req);
        return pdfResponse(pdf.a4(req, totals, inv, date(now)), inv + "-A4.pdf");
    }

    /** widthMm: 80 (default) or 58 for small thermal printers */
    @PostMapping("/pos")
    public ResponseEntity<byte[]> pos(@Valid @RequestBody BillRequest req,
                                      @RequestParam(defaultValue = "80") int widthMm) {
        var now = LocalDateTime.now();
        String inv = invoiceNo(req, now);
        var totals = BillCalculator.compute(req);
        return pdfResponse(pdf.pos(req, totals, inv, date(now), widthMm == 58 ? 58 : 80), inv + "-POS.pdf");
    }

    private static String invoiceNo(BillRequest r, LocalDateTime now) {
        return (r.invoiceNo() == null || r.invoiceNo().isBlank())
                ? "INV-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                : r.invoiceNo().trim();
    }

    private static String date(LocalDateTime now) {
        return now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }

    private static ResponseEntity<byte[]> pdfResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString())
                .body(bytes);
    }
}
