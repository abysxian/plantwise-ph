package com.agristep.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a plain text report as a valid multi-page PDF (A4, Helvetica) with no outside library.
 * Characters outside basic Latin-1 (emoji, for example) are replaced by "?".
 */
final class SimplePdf {
    private static final int PAGE_W = 595, PAGE_H = 842, MARGIN = 54;
    private static final int LINE = 14, TITLE_LINE = 24;
    private static final int MAX_CHARS = 92;

    private final List<String[]> lines = new ArrayList<>();       // {style, text}; style "T" title, "H" heading, "B" body

    void title(String text) { lines.add(new String[] {"T", text}); }
    void heading(String text) { lines.add(new String[] {"H", text}); }
    void blank() { lines.add(new String[] {"B", ""}); }

    void body(String text) {
        for (String wrapped : wrap(text)) lines.add(new String[] {"B", wrapped});
    }

    void write(Path file) throws IOException {
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        int y = PAGE_H - MARGIN;
        for (String[] l : lines) {
            int step = "T".equals(l[0]) ? TITLE_LINE : LINE;
            if (y - step < MARGIN) {
                pages.add(page.toString());
                page = new StringBuilder();
                y = PAGE_H - MARGIN;
            }
            y -= step;
            if (!l[1].isEmpty()) {
                String font = "B".equals(l[0]) ? "/F1 10" : "/F2 " + ("T".equals(l[0]) ? "18" : "12");
                page.append("BT ").append(font).append(" Tf ").append(MARGIN).append(' ').append(y).append(" Td (")
                        .append(escape(l[1])).append(") Tj ET\n");
            }
        }
        pages.add(page.toString());

        // objects: 1 catalog, 2 pages, 3 font F1, 4 font F2, then per page: page object and content stream
        List<byte[]> objects = new ArrayList<>();
        objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) kids.append(5 + i * 2).append(" 0 R ");
        objects.add(bytes("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));
        for (int i = 0; i < pages.size(); i++) {
            int contentObj = 6 + i * 2;
            objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_W + " " + PAGE_H + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentObj + " 0 R >>"));
            byte[] stream = pages.get(i).getBytes(StandardCharsets.ISO_8859_1);
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            o.write(bytes("<< /Length " + stream.length + " >>\nstream\n"));
            o.write(stream);
            o.write(bytes("\nendstream"));
            objects.add(o.toByteArray());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(bytes("%PDF-1.4\n"));
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            out.write(bytes((i + 1) + " 0 obj\n"));
            out.write(objects.get(i));
            out.write(bytes("\nendobj\n"));
        }
        int xref = out.size();
        out.write(bytes("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
        for (int off : offsets) out.write(bytes(String.format("%010d 00000 n \n", off)));
        out.write(bytes("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n"));
        Files.write(file, out.toByteArray());
    }

    private static List<String> wrap(String text) {
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            String rest = paragraph;
            while (rest.length() > MAX_CHARS) {
                int cut = rest.lastIndexOf(' ', MAX_CHARS);
                if (cut <= 0) cut = MAX_CHARS;
                result.add(rest.substring(0, cut));
                rest = rest.substring(cut).stripLeading();
            }
            result.add(rest);
        }
        return result;
    }

    private static String escape(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') sb.append('\\').append(c);
            else if (c == '°') sb.append('°');                  // degree sign exists in Latin-1
            else if (c < 32 || c > 255) sb.append('?');
            else sb.append(c);
        }
        return sb.toString();
    }

    private static byte[] bytes(String s) { return s.getBytes(StandardCharsets.ISO_8859_1); }
}
