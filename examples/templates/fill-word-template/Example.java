package examples.templates.fill_word_template;

import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] filled = client.fillTemplate("sales_report", Map.of(
            "reportTitle", "Quarterly Sales",
            "date", "2024-04-01",
            "items", List.of(
                Map.of("product", "Widget A", "sales", 100, "revenue", 5000),
                Map.of("product", "Widget B", "sales", 200, "revenue", 8000),
                Map.of("product", "Widget C", "sales", 50, "revenue", 2500)
            ),
            "summary", Map.of("totalRevenue", 15500, "growth", "15%")
        ));

        Files.write(Path.of("filled_report.docx"), filled);
        System.out.println("Saved filled_report.docx");
        // docs:end
    }
}
