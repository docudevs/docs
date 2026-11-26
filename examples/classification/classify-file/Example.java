package examples.classification.classify_file;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ClassifyFileOptions;
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
        long caseId = 42L; // id of a records case with an active category tree

        // Compresses the file and uploads it in blocks. The columns are the source column names
        // of the dataset, and an uploaded file needs a token budget.
        // Throws DocuDevsApiException (409) when the job fails or is cancelled, and
        // DocuDevsTimeoutException after 3600 seconds.
        JsonNode job = client.classifyAndWait(
                caseId,
                Path.of("weekly.csv"),
                ClassifyFileOptions.builder().maxTotalTokens(5_000_000L).build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(3600))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());
        System.out.println(job.path("jobId").asText() + " " + job.path("status").asText()
                + " " + job.path("counts"));
        // docs:end
    }
}
