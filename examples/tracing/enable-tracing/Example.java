package examples.tracing.enable_tracing;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] fileBytes = Files.readAllBytes(Path.of("invoice.pdf"));
        UploadRequest upload = new UploadRequest("invoice.pdf", "application/pdf", fileBytes);

        String guid = client.submitAndProcessDocument(
                upload,
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract invoice data")
                        .trace(true)
                        .build());

        client.waitUntilReadyJson(guid, WaitOptions.builder().build());

        JsonNode trace = client.getTrace(guid);
        System.out.println("Total tokens used: " + trace.path("total_tokens").asInt());
        System.out.println("LLM calls: " + trace.path("total_llm_calls").asInt());
    }
}
// docs:end
