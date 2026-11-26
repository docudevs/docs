package examples.classification.get_classify_results;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ClassifyResultFilters;
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

        // Follows the continuation tokens until every matching row was read, one page at a time
        client.forEachClassifyResult(
                caseId,
                jobId,
                ClassifyResultFilters.builder().reviewRequired(true).build(),
                null,
                row -> System.out.println(row.path("rowId").asText() + " " + row.path("key").asText(null)
                        + " " + row.path("chosen").path("code").asText(null)
                        + " " + row.path("review").path("reasons")));
        // docs:end
    }
}
