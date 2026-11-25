package examples.extraction.generate_schema;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
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
        byte[] documentData = Files.readAllBytes(Path.of("sample-invoice.pdf"));
        UploadRequest upload = new UploadRequest("sample-invoice.pdf", "application/pdf", documentData);

        String jobGuid = client.generateSchema(
                upload,
                "Focus on extracting the invoice number, date, and all line items with prices.");

        JsonNode schema = client.waitUntilReadyJson(jobGuid, WaitOptions.builder().build());
        System.out.println("Generated schema:");
        System.out.println(schema.toPrettyString());
        // docs:end
    }
}
