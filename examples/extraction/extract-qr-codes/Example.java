package examples.extraction.extract_qr_codes;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .build();

        byte[] fileBytes = Files.readAllBytes(Path.of("invoice.pdf"));
        UploadRequest upload = new UploadRequest("invoice.pdf", "application/pdf", fileBytes);

        String guid = client.submitAndProcessDocument(
                upload,
                ProcessOptions.builder()
                        .prompt("Extract QR codes")
                        .mimeType("application/pdf")
                        .barcodes(true)
                        .build());

        JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.builder().build());
        System.out.println(result);
    }
}
// docs:end
