package examples.records.get_record;

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

        JsonNode record = client.getRecord(caseId, "1000000000001");
        System.out.println(record.path("label").asText(null) + " " + record.path("labelPath")
                + " " + record.path("labelSource").asText(null));
        System.out.println(record.path("chunk"));
        // docs:end
    }
}
