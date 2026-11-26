package examples.operations.submit_pdf_acroform;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String parentJobGuid = "<job-guid>"; // GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

        JsonNode response = client.submitPdfAcroformOperation(parentJobGuid, null);

        System.out.println("Operation job GUID: " + response.path("jobGuid").asText());
        System.out.println("Status: " + response.path("status").asText());
        // docs:end
    }
}
