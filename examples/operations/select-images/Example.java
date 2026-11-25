package examples.operations.select_images;

import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;

// docs:start
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        ObjectMapper mapper = new ObjectMapper();
        String jobGuid = "<job-guid>"; // GUID of a completed job processed with figure extraction

        client.submitOperation(jobGuid, "image-select", null,
                Map.of(
                        "prompt", "Return all diagrams from the document",
                        "topK", 5,
                        "matchMode", "all",
                        "useVision", false));

        JsonNode result = client.waitForOperation(
                jobGuid, "image-select", WaitOptions.builder().build());

        JsonNode payload = mapper.readTree(result.path("result").asText());
        System.out.println(payload.toPrettyString());

        JsonNode first = payload.path("selected").get(0);
        if (first != null) {
            String imageId = first.path("id").asText();
            byte[] imageBytes = client.getFigureImage(jobGuid, imageId);
            Files.write(Path.of("selected_figure.png"), imageBytes);
        }
        // docs:end
    }
}
