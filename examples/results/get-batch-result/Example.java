package examples.results.get_batch_result;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String batchGuid = "<batch-guid>"; // GUID of a batch whose processing has finished

        // Batch overview: state and one entry per document
        JsonNode overview = new ObjectMapper().readTree(client.fetchResult(batchGuid));
        System.out.println("state: " + overview.path("state").asText()
                + ", completed: " + overview.path("completed").asInt(0)
                + ", failed: " + overview.path("failed").asInt(0));
        for (JsonNode document : overview.path("documents")) {
            System.out.println("document " + document.path("index").asInt()
                    + ": " + document.path("status").asText());
        }

        // Only the results, in upload order; a document without a result is null
        JsonNode results = client.fetchResultJson(batchGuid);
        System.out.println(results.toPrettyString());
        // docs:end
    }
}
