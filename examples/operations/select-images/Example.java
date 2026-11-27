package examples.operations.select_images;

import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;

// docs:start
import ai.docudevs.client.ImageSelectionOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String jobGuid = "<job-guid>"; // GUID of a completed job processed with figure extraction

        // One call submits the operation and waits for it; it returns the operation's result document
        JsonNode payload = client.submitAndWaitForImageSelection(
                jobGuid,
                "Return all diagrams from the document",
                ImageSelectionOptions.builder()
                        .topK(5)
                        .matchMode("all")
                        .useVision(false)
                        .build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(120))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());
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
