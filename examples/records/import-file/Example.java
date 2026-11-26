package examples.records.import_file;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.RecordUpsertImportOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.nio.file.Path;
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case with a configured dataset

        // Compresses the file and uploads it in blocks. Throws DocuDevsApiException (409) when the
        // import fails, and DocuDevsTimeoutException after 1800 seconds.
        JsonNode result = client.bulkUpsertRecordsAndWait(
                caseId,
                Path.of("products.csv"), // columns as named in the dataset field mapping
                RecordUpsertImportOptions.builder().build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(1800))
                        .pollInterval(Duration.ofSeconds(2))
                        .build(),
                System.out::println);
        System.out.println(result.path("status").asText() + " " + result.path("rowsReceived").asText()
                + " " + result.path("rowsRejected").asText());
        System.out.println(result.path("recordsAdded").asText() + " " + result.path("recordsUpdated").asText()
                + " " + result.path("recordsUnchanged").asText());
        // docs:end
    }
}
