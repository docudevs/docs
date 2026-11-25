package examples.cases.list_summaries;

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

        JsonNode summaries = client.listCaseSummaries(caseId);
        for (JsonNode doc : summaries) {
            System.out.println(doc.path("filename").asText() + ": "
                    + doc.path("summary").path("document_type").asText());
        }
        // docs:end
    }
}
