package examples.lookup_files.upload_excel_template;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.UploadRequest;
import java.nio.file.Files;
import java.nio.file.Path;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] templateData = Files.readAllBytes(Path.of("template.xlsx"));

        // The Java client returns no status code: the call throws DocuDevsApiException unless the API answers 2xx
        client.uploadExcelTemplate(
                "invoice-config",
                new UploadRequest(
                        "template.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        templateData));
        System.out.println("Uploaded template.xlsx");
        // docs:end
    }
}
