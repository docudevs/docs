package examples.records.record_stats;

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
        long caseId = 42L; // id of a records case

        JsonNode stats = client.recordStats(caseId);
        System.out.println(stats.path("records").asText() + " records, "
                + stats.path("unlabelled").asText() + " without a label");
        for (var counts : stats.path("labels").properties()) {
            System.out.println(counts.getKey() + " " + counts.getValue());
        }
        // docs:end
    }
}
