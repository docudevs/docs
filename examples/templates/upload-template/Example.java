package examples.templates.upload_template;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.UploadRequest;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] fileBytes = Files.readAllBytes(Path.of("invoice_template.pdf"));
        UploadRequest upload = new UploadRequest("invoice_template.pdf", "application/pdf", fileBytes);

        client.uploadTemplate("invoice", upload);
        System.out.println("Template uploaded.");
    }
}
// docs:end
