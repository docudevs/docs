package examples.records.import_from_url;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.RecordUrlImportOptions;
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
        long caseId = 42L; // id of a records case with a configured dataset

        // The worker fetches the file, so the URL must be https and publicly reachable.
        // Throws DocuDevsApiException (409) when the import fails, and DocuDevsTimeoutException after 1800 seconds.
        JsonNode result = client.importRecordsFromUrlAndWait(
                caseId,
                "https://example.com/exports/products.csv",
                "csv",
                RecordUrlImportOptions.builder().build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(1800))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());
        System.out.println(result.path("status").asText() + " " + result.path("rowsReceived").asText()
                + " " + result.path("recordsAdded").asText());
        // docs:end
    }
}
