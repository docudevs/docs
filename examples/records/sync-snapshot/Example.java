package examples.records.sync_snapshot;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.RecordSyncOptions;
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

        Path snapshot = Path.of("snapshot.ndjson");
        RecordSyncOptions syncOptions = RecordSyncOptions.builder().build();
        WaitOptions waitOptions = WaitOptions.builder()
                .timeout(Duration.ofSeconds(1800))
                .pollInterval(Duration.ofSeconds(2))
                .build();

        // A dry run: it changes nothing and produces the report.
        // Throws DocuDevsApiException (409) when an import fails, and DocuDevsTimeoutException after 1800 seconds.
        JsonNode dry = client.syncRecordsAndWait(caseId, snapshot, syncOptions, waitOptions);
        JsonNode summary = dry.path("syncSummary");
        if (summary.isMissingNode() || summary.isNull()) {
            throw new IllegalStateException("The dry run produced no report");
        }
        System.out.println(summary.path("counts"));
        System.out.println(summary.path("guard"));

        // After reviewing the report, apply the same file for real
        JsonNode applied = client.confirmSyncRecordsAndWait(
                caseId, snapshot, dry.path("importId").asLong(), syncOptions, waitOptions);
        System.out.println(applied.path("status").asText() + " " + applied.path("recordsAdded").asText()
                + " " + applied.path("recordsDeleted").asText());
        // docs:end
    }
}
