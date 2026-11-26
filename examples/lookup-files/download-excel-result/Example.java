package examples.lookup_files.download_excel_result;

import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String jobGuid = "<job-guid>"; // GUID of a completed extraction job

        byte[] xlsxBytes = client.fetchResultExcelBytes(jobGuid, "invoice-config");
        Files.write(Path.of("invoices.xlsx"), xlsxBytes);
        System.out.println("Saved " + xlsxBytes.length + " bytes to invoices.xlsx");
        // docs:end
    }
}
