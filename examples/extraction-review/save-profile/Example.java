package examples.extraction_review.save_profile;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        // The configuration invoice-v1 must already exist.
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        schema.putObject("properties").putObject("total").put("type", "number");

        ObjectNode previewRequest = mapper.createObjectNode().put("schemaJson", mapper.writeValueAsString(schema));
        previewRequest.putNull("overlay");
        JsonNode preview = client.previewExtractionReviewProfile("invoice-v1", previewRequest);

        ObjectNode saveRequest = mapper.createObjectNode()
                .put("expectedRevision", preview.path("revision").asLong())
                .put("enabled", true)
                .put("schemaJson", preview.path("schemaJson").asText());
        saveRequest.putNull("overlay");
        JsonNode saved = client.saveExtractionReviewProfile("invoice-v1", saveRequest);

        System.out.println("Saved revision " + saved.path("revision").asLong()
                + ", enabled: " + saved.path("enabled").asBoolean());
        // docs:end
    }
}
