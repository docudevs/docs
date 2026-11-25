package examples.extraction.process_page_range;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] fileBytes = Files.readAllBytes(Path.of("document.pdf"));
        UploadRequest upload = new UploadRequest("document.pdf", "application/pdf", fileBytes);

        // Process only page 1
        String guid = client.submitAndProcessDocument(
                upload,
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract the title and summary.")
                        .pageRange(List.of(1))
                        .build());

        JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.builder().build());
        System.out.println(result.toPrettyString());
    }
}
// docs:end
