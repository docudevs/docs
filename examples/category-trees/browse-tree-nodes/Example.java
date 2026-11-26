package examples.category_trees.browse_tree_nodes;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.TreeNodeOptions;
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
        long versionId = 7L; // treeVersionId returned when the tree was uploaded

        JsonNode roots = client.browseTreeNodes(caseId, versionId);
        JsonNode children = client.browseTreeNodes(
                caseId, versionId, TreeNodeOptions.builder().parent("4.12").build());
        JsonNode hits = client.browseTreeNodes(
                caseId, versionId, TreeNodeOptions.builder().query("sock").build());

        for (JsonNode node : hits.path("items")) {
            System.out.println(node.path("code").asText() + " " + node.path("name").asText()
                    + " " + node.path("status").asText() + " " + node.path("childCount").asText());
        }
        System.out.println(hits.path("total").asText() + " matches");
        System.out.println(roots.path("items").size() + " roots");
        System.out.println(children.path("items").size() + " children of 4.12");
        // docs:end
    }
}
