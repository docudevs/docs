package examples.category_trees.upload_tree;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.DocuDevsApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// docs:end
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case

        // The root node has no parent; the field is left out
        List<Map<String, String>> nodes = List.of(
                Map.of("code", "4", "name", "Clothing"),
                Map.of("code", "4.12", "parent", "4", "name", "Socks and tights"),
                Map.of("code", "4.12.3", "parent", "4.12", "name", "Socks",
                        "description", "Socks worn inside shoes"),
                Map.of("code", "9.90.1", "parent", "9.90", "name", "OLD Socks",
                        "status", "retired", "successor", "4.12.3"));

        JsonNode created;
        try {
            created = client.uploadTree(caseId, nodes, "Catalogue 2026-10");
        } catch (DocuDevsApiException error) {
            // A rejected tree stores nothing; its body lists every problem
            JsonNode body = new ObjectMapper().readTree(error.getResponseBody());
            for (JsonNode problem : body.path("problems")) {
                System.out.println(problem.path("type").asText() + " " + problem.path("code").asText(null)
                        + " " + problem.path("message").asText());
            }
            throw error;
        }
        System.out.println("Tree version " + created.path("treeVersionId").asText() + ": "
                + created.path("status").asText() + ", " + created.path("nodes").asText() + " nodes");
        // docs:end
    }
}
