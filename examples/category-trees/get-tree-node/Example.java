package examples.category_trees.get_tree_node;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.util.ArrayList;
import java.util.List;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case
        long versionId = 7L; // treeVersionId returned when the tree was uploaded

        JsonNode node = client.getTreeNode(caseId, versionId, "4.12.3");

        List<String> path = new ArrayList<>();
        for (JsonNode name : node.path("path")) {
            path.add(name.asText());
        }
        System.out.println(String.join(" > ", path));

        List<String> childCodes = new ArrayList<>();
        for (JsonNode child : node.path("children")) {
            childCodes.add(child.path("code").asText());
        }
        System.out.println(childCodes);

        List<String> siblingCodes = new ArrayList<>();
        for (JsonNode sibling : node.path("siblings")) {
            siblingCodes.add(sibling.path("code").asText());
        }
        System.out.println(siblingCodes);
        // docs:end
    }
}
