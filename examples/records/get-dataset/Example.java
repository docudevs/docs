package examples.records.get_dataset;

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

        JsonNode dataset = client.getDataset(caseId);
        System.out.println("Mapping version: " + dataset.path("mappingVersion").asText());
        System.out.println("Active tree version: " + dataset.path("activeTreeVersionId").asText(null));
        // docs:end
    }
}
