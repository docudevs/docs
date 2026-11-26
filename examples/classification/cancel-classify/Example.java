package examples.classification.cancel_classify;

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
        long caseId = 42L; // id of a records case
        long jobId = 9L; // jobId returned when the job was started

        // Throws DocuDevsApiException (409) when the job has already finished
        JsonNode job = client.cancelClassify(caseId, jobId);
        System.out.println(job.path("status").asText());
        // docs:end
    }
}
