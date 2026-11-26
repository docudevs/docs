package examples.case_search.search_documents;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.CaseSearchOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a case that holds documents and no records dataset

        JsonNode docs = client.searchDocuments(
                caseId,
                "termination notice period",
                CaseSearchOptions.builder().topK(5).build());

        for (JsonNode result : docs.path("results")) {
            System.out.println(result.path("filename").asText(null) + " page " + result.path("pageNumber").asText(null));
        }
        // docs:end
    }
}
