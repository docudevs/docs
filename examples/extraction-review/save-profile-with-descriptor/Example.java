package examples.extraction_review.save_profile_with_descriptor;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
// docs:end
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        // descriptor.json is written by the previous example, which builds it in Python.
        // The configuration invoice-v1 must already exist.
        String descriptorJson = Files.readString(Path.of("descriptor.json"));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode preview = client.previewExtractionReviewProfile(
                "invoice-v1",
                mapper.createObjectNode().put("clientPipelineJson", descriptorJson));

        ObjectNode saveRequest = mapper.createObjectNode()
                .put("expectedRevision", preview.path("revision").asLong())
                .put("clientPipelineJson", preview.path("clientPipelineJson").asText())
                .put("enabled", true);
        JsonNode saved = client.saveExtractionReviewProfile("invoice-v1", saveRequest);

        System.out.println("Saved revision " + saved.path("revision").asLong());
        System.out.println("Producer fingerprint: " + saved.path("producerFingerprint").asText());
        // docs:end
    }
}
