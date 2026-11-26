package examples.records.list_records;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.RecordListOptions;
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

        // Follows the continuation tokens until every matching record was read
        JsonNode records = client.listRecords(
                caseId,
                RecordListOptions.builder()
                        .subtree("4.12")
                        .attributes(Map.of("brand", "Foo"))
                        .build());
        for (JsonNode record : records) {
            System.out.println(record.path("key").asText() + " " + record.path("label").asText(null));
        }
        // docs:end
    }
}
