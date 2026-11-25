package examples.cases.delete_document;

import ai.docudevs.client.DocuDevsClient;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 123L; // id returned by createCase
        String documentId = "doc-uuid-here"; // documentId from listCaseDocuments

        client.deleteCaseDocument(caseId, documentId);
        System.out.println("Document deleted successfully");
        // docs:end
    }
}
