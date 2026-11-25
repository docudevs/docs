package examples.cases.list_documents;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 123L; // id returned by createCase

        JsonNode documentsPage = client.listCaseDocuments(caseId, 0, 20);

        System.out.println("Total documents: " + documentsPage.path("totalSize").asInt());
        System.out.println("Page: " + documentsPage.path("pageNumber").asInt() + "/" + documentsPage.path("totalPages").asInt());

        for (JsonNode document : documentsPage.path("content")) {
            System.out.println(document.path("filename").asText() + " [" + document.path("documentId").asText() + "]");
            System.out.println("  status=" + document.path("processingStatus").asText());
        }
        // docs:end
    }
}
