package examples.knowledge_search.extract_with_records_search;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.ToolDescriptor;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String caseId = "51"; // id of the records knowledge base, from listKnowledgeBases

        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "lines": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "description": {"type": "string"},
                          "product_key": {"type": "string"}
                        }
                      }
                    }
                  }
                }
                """;

        ToolDescriptor tool = new ToolDescriptor("RECORDS_SEARCH", null, Map.of("caseId", caseId, "topK", 5));

        byte[] documentData = Files.readAllBytes(Path.of("invoice.pdf"));

        String jobId = client.submitAndProcessDocument(
                new UploadRequest("invoice.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("For each invoice line, fill product_key with the catalogue record that matches the item.")
                        .schema(schema)
                        .tools(List.of(tool))
                        .build());

        JsonNode result = client.waitUntilReadyJson(
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println(result.toPrettyString());
        // docs:end
    }
}
