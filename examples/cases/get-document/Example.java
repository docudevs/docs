package examples.cases.get_document;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("DOCUDEVS_API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 123L; // id returned by createCase
        String documentId = "doc-uuid-here"; // documentId from listCaseDocuments

        JsonNode document = client.getCaseDocument(caseId, documentId);

        System.out.println("Document: " + document.path("filename").asText());
        System.out.println("Content Type: " + document.path("contentType").asText());
        System.out.println("Processing Status: " + document.path("processingStatus").asText());
        System.out.println("Metadata: " + document.path("metadata"));
        // docs:end
    }
}
