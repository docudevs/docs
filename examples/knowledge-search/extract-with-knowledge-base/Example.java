package examples.knowledge_search.extract_with_knowledge_base;

import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// docs:start
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.ToolDescriptor;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String caseId = "42"; // id of the knowledge base, from GET /knowledge-bases

        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "records": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "sku": {"type": "string"},
                          "canonical_name": {"type": "string"},
                          "localized_display_name": {"type": "string", "maxLength": 40},
                          "product_group": {"type": "string"},
                          "marketing_summary": {"type": "string"}
                        },
                        "required": ["sku", "canonical_name", "localized_display_name"]
                      }
                    }
                  },
                  "required": ["records"]
                }
                """;

        byte[] fileBytes = Files.readAllBytes(Path.of("products.csv"));
        UploadRequest upload = new UploadRequest("products.csv", "text/csv", fileBytes);

        ProcessOptions options = ProcessOptions.builder()
                .mimeType("text/csv")
                .prompt("Fill canonical_name, localized_display_name (at most 40 characters), "
                        + "product_group and marketing_summary for every SKU, "
                        + "using the Product Catalogue knowledge base.")
                .schema(schema)
                .tools(List.of(ToolDescriptor.knowledgeBaseSearch(caseId, 8)))
                .build();

        String guid = client.submitAndProcessDocument(upload, options);
        JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.defaults());
        System.out.println(result);
        // docs:end
    }
}
