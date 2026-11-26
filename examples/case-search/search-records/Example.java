package examples.case_search.search_records;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.CaseSearchOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case

        JsonNode hits = client.searchRecords(
                caseId,
                "Soft sports sock",
                CaseSearchOptions.builder()
                        .topK(10)
                        .subtree("4.12")
                        .attributes(Map.of("brand", "Foo"))
                        .build());

        for (JsonNode result : hits.path("results")) {
            System.out.println(result.path("key").asText() + " " + result.path("label").asText(null)
                    + " " + result.path("score").asText(null));
        }
        // docs:end
    }
}
