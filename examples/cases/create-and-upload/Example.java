package examples.cases.create_and_upload;

import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.UploadRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("DOCUDEVS_API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        // Step 1: Create a case first
        JsonNode created = client.createCase("Contract Review", "Contract documents for review");
        long caseId = created.path("id").asLong();

        // Step 2: Upload document to case
        byte[] documentData = Files.readAllBytes(Path.of("contract.pdf"));
        client.uploadCaseDocument(caseId, new UploadRequest("contract.pdf", "application/pdf", documentData));

        System.out.println("Uploaded document to case " + caseId);
        // docs:end
    }
}
