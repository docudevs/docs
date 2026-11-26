package examples.operations.get_document_outline;

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
        String jobGuid = "<job-guid>"; // GUID of a job that already has an outline artifact

        JsonNode outline = client.getDocumentOutline(jobGuid);
        System.out.println(outline.toPrettyString());
        // docs:end
    }
}
