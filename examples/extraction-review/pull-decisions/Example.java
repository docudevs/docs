package examples.extraction_review.pull_decisions;

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
        long cursor = 0L; // nextCursor from the previous pull; 0 for the first pull

        JsonNode page = client.pullExtractionReviewDecisions(cursor, 100, null);

        for (JsonNode event : page.path("events")) {
            System.out.println(event.path("cursorOrdinal").asLong() + " "
                    + event.path("eventId").asText() + " " + event.path("kind").asText());
        }

        // Persist nextCursor exactly as returned, even when events is empty
        cursor = page.path("nextCursor").asLong();
        System.out.println("Next cursor: " + cursor);
        // docs:end
    }
}
