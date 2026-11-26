package examples.records.wait_for_import;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case
        long importId = 301L; // importId returned when the import was created

        // Throws DocuDevsApiException (409) when the import fails, and DocuDevsTimeoutException after 1800 seconds
        JsonNode finished = client.waitForImport(
                caseId,
                importId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(1800))
                        .pollInterval(Duration.ofSeconds(2))
                        .build(),
                System.out::println);
        System.out.println(finished.path("status").asText() + " " + finished.path("recordsAdded").asText()
                + " " + finished.path("recordsUpdated").asText());
        // docs:end
    }
}
