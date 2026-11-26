package examples.results.download_excel_with_template;

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

        // invoice-config is a saved configuration that has an Excel template attached.
        // Name the output file .xlsm when the template is macro-enabled.
        byte[] workbook = client.fetchResultExcelBytes(jobGuid, "invoice-config");
        Files.write(Path.of("result.xlsx"), workbook);
        // docs:end
    }
}
