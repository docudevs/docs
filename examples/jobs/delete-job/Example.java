package examples.jobs.delete_job;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        String jobGuid = "<job-guid>"; // GUID of a completed job

        JsonNode purgeResult = client.deleteJob(jobGuid);

        System.out.println("Deleted " + purgeResult.path("jobsDeleted").asInt() + " job(s)");
    }
}
// docs:end
