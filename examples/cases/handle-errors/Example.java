package examples.cases.handle_errors;

import ai.docudevs.client.DocuDevsApiException;
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.UploadRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId;
        try {
            JsonNode created = client.createCase("Contract Review", null);
            caseId = created.path("id").asLong();
            System.out.println("Created case: " + caseId);
        } catch (DocuDevsApiException e) {
            System.out.println("Error creating case: HTTP " + e.getStatusCode() + " " + e.getResponseBody());
            return;
        }

        try {
            byte[] documentData = Files.readAllBytes(Path.of("contract.pdf"));
            client.uploadCaseDocument(caseId, new UploadRequest("contract.pdf", "application/pdf", documentData));
            System.out.println("Uploaded document to case");
        } catch (DocuDevsApiException e) {
            System.out.println("Error uploading document: HTTP " + e.getStatusCode() + " " + e.getResponseBody());
        }
        // docs:end
    }
}
