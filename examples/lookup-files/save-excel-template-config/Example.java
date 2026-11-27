package examples.lookup_files.save_excel_template_config;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.ExcelTemplateOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import java.util.List;
import java.util.Map;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String schema = Schema.of(Map.of(
                "type", "object",
                "properties", Map.of(
                        "vendor_name", Map.of("type", "string"),
                        "invoice_number", Map.of("type", "string"),
                        "total_amount", Map.of("type", "number")),
                "required", List.of("total_amount")));

        // The Java client returns no status code: the call throws DocuDevsApiException unless the API answers 2xx
        client.saveConfiguration(
                "invoice-config",
                ProcessOptions.builder()
                        .prompt("Extract the vendor, invoice number and total of each invoice.")
                        .schema(schema)
                        // startRow and startCol are 0-based cell coordinates: 0 and 0 target cell A1
                        .excelTemplate(ExcelTemplateOptions.builder()
                                .startRow(0)
                                .startCol(0)
                                .includeHeaders(true)
                                .sheetName("Data")
                                .build())
                        .build());
        System.out.println("Configuration saved");
        // docs:end
    }
}
