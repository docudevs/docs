package examples.cases.get_summary;

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
        long caseId = 123L; // id returned by createCase
        String documentId = "doc-uuid-here"; // documentId from listCaseDocuments

        JsonNode summary = client.getDocumentSummary(caseId, documentId);
        System.out.println(summary.path("filename").asText());
        System.out.println(summary.path("summary").path("document_type").asText());
        System.out.println(summary.path("summary").path("summary").asText());
        // docs:end
    }
}
