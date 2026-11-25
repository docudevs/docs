package examples.configuration.process_with_configuration;

import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;

// docs:start
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] fileBytes = Files.readAllBytes(Path.of("invoice.pdf"));
        UploadRequest upload = new UploadRequest("invoice.pdf", "application/pdf", fileBytes);

        String guid = client.uploadDocument(upload);
        client.processDocumentWithConfiguration(guid, "invoice-config");

        JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.defaults());
        System.out.println(result);
        // docs:end
    }
}
