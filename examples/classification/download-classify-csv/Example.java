package examples.classification.download_classify_csv;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ClassifyResultFilters;
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
        long caseId = 42L; // id of a records case
        long jobId = 9L; // jobId returned when the job was started

        byte[] csv = client.downloadClassifyCsv(
                caseId, jobId, ClassifyResultFilters.builder().flag("declined").build());
        Path path = Files.write(Path.of("declined.csv"), csv);
        System.out.println("Wrote " + path);
        // docs:end
    }
}
