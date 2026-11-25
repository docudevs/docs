package examples.cases.list;

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
        JsonNode cases = client.listCases();

        for (JsonNode item : cases) {
            System.out.println("Case " + item.path("id").asLong() + ": " + item.path("name").asText());
        }
        // docs:end
    }
}
