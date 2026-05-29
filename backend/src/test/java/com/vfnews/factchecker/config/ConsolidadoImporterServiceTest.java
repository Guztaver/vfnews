package com.vfnews.factchecker.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsolidadoImporterServiceTest {

    @Test
    void shouldParseSimpleCSV() {
        String csv = "texto,label,fonte,data_coleta\n"
            + "bolsonaro faz reuniao com apoiadores,verdadeiro,CNN,2026-05-16\n"
            + "beber cloro cura doencas graves,falso,Lupa,2026-05-16\n";

        List<String[]> rows = ConsolidadoImporterService.parseCSV(csv);

        assertEquals(3, rows.size(), "Should have header + 2 data rows");
        assertEquals("texto", rows.get(0)[0]);
        assertEquals("label", rows.get(0)[1]);
        assertEquals("fonte", rows.get(0)[2]);

        assertEquals("bolsonaro faz reuniao com apoiadores", rows.get(1)[0]);
        assertEquals("verdadeiro", rows.get(1)[1]);
        assertEquals("CNN", rows.get(1)[2]);

        assertEquals("beber cloro cura doencas graves", rows.get(2)[0]);
        assertEquals("falso", rows.get(2)[1]);
        assertEquals("Lupa", rows.get(2)[2]);
    }

    @Test
    void shouldParseQuotedFieldsNoNewline() {
        String csv = "texto,label,fonte,data_coleta\n"
            + "\"ciao, mondo\",verdadeiro,Test,2026-01-01\n";

        List<String[]> rows = ConsolidadoImporterService.parseCSV(csv);

        assertEquals(2, rows.size());
        assertEquals("ciao, mondo", rows.get(1)[0],
            "Comma inside quotes should be preserved as part of the field");
    }

    @Test
    void shouldParseQuotedFieldsWithNewlines() {
        String csv = "texto,label,fonte,data_coleta\n"
            + "\"line one\nline two\",falso,Source,2026-01-01\n";

        List<String[]> rows = ConsolidadoImporterService.parseCSV(csv);

        assertEquals(2, rows.size());
        assertEquals("line one\nline two", rows.get(1)[0],
            "Newlines inside quotes should be preserved");
        assertEquals("falso", rows.get(1)[1]);
        assertEquals("Source", rows.get(1)[2]);
    }

    @Test
    void shouldParseEscapedQuotes() {
        String csv = "texto,label,fonte,data_coleta\n"
            + "\"ele disse \"\"ola\"\"\",verdadeiro,Test,2026-01-01\n";

        List<String[]> rows = ConsolidadoImporterService.parseCSV(csv);

        assertEquals(2, rows.size());
        assertEquals("ele disse \"ola\"", rows.get(1)[0],
            "Escaped quotes should be unescaped");
    }

    @Test
    void shouldParseMultipleMultiLineRows() {
        String csv = "texto,label,fonte,data_coleta\n"
            + "\"row1\nline2\",verdadeiro,CNN,2026-01-01\n"
            + "\"row2\nline2\",falso,Lupa,2026-01-02\n";

        List<String[]> rows = ConsolidadoImporterService.parseCSV(csv);

        assertEquals(3, rows.size());
        assertEquals("row1\nline2", rows.get(1)[0]);
        assertEquals("verdadeiro", rows.get(1)[1]);
        assertEquals("row2\nline2", rows.get(2)[0]);
        assertEquals("falso", rows.get(2)[1]);
    }

    @Test
    void shouldParseRealDataset() throws Exception {
        // Load the actual dataset from classpath
        InputStream is = getClass()
            .getClassLoader()
            .getResourceAsStream("datasets/dataset_consolidado.csv");

        assertNotNull(is, "dataset_consolidado.csv should be accessible from classpath");

        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        List<String[]> rows = ConsolidadoImporterService.parseCSV(content);

        assertTrue(rows.size() > 100,
            "Should parse at least 100 rows from the real dataset, got " + rows.size());

        // First row should be the header
        String[] header = rows.get(0);
        assertEquals("texto", header[0].trim());
        assertEquals("label", header[1].trim());
        assertEquals("fonte", header[2].trim());

        // Count valid rows
        int validRows = 0;
        int verdadeiroCount = 0;
        int falsoCount = 0;
        for (int i = 1; i < rows.size(); i++) {
            String[] cols = rows.get(i);
            if (cols.length >= 3 && !cols[0].isBlank()) {
                validRows++;
                String label = cols[1];
                if ("verdadeiro".equalsIgnoreCase(label.trim())) {
                    verdadeiroCount++;
                } else if ("falso".equalsIgnoreCase(label.trim())) {
                    falsoCount++;
                }
            }
        }

        System.out.println("Total rows parsed: " + rows.size());
        System.out.println("Valid data rows: " + validRows);
        System.out.println("Verdadeiro: " + verdadeiroCount);
        System.out.println("Falso: " + falsoCount);

        assertTrue(validRows > 1000,
            "Should find at least 1000 valid rows, got " + validRows);
        assertTrue(verdadeiroCount > 100,
            "Should find some 'verdadeiro' entries, got " + verdadeiroCount);
        assertTrue(falsoCount > 100,
            "Should find some 'falso' entries, got " + falsoCount);
    }

    @Test
    void simplifyLabelShouldMapCorrectly() {
        assertEquals("false", ConsolidadoImporterService.simplifyLabel("falso"));
        assertEquals("false", ConsolidadoImporterService.simplifyLabel("FALSO"));
        assertEquals("false", ConsolidadoImporterService.simplifyLabel("fAlSe"));
        assertEquals("true", ConsolidadoImporterService.simplifyLabel("verdadeiro"));
        assertEquals("true", ConsolidadoImporterService.simplifyLabel("VERDADEIRO"));
    }
}
