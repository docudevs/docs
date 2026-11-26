package examples.layout.get_document_layout;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.util.List;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String jobGuid = "<job-guid>"; // GUID of a completed job that was processed with OCR

        // Only page 1, and only its words and paragraphs
        JsonNode layout = client.getDocumentLayout(jobGuid, null, "1", List.of("words", "paragraphs"));
        JsonNode quality = client.getOcrQuality(jobGuid);

        // Both methods throw DocuDevsApiException with status 404 when the artifact does not exist
        System.out.println(layout.toPrettyString());
        System.out.println(quality.toPrettyString());
        // docs:end
    }
}
