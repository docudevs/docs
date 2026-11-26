package examples.category_trees.list_trees;

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

        JsonNode versions = client.listTrees(caseId);
        for (JsonNode version : versions.path("items")) {
            System.out.println(version.path("treeVersionId").asText() + " " + version.path("status").asText()
                    + " " + version.path("nodes").asText() + " " + version.path("levels").asText());
        }
        // docs:end
    }
}
