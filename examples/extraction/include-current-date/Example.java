package examples.extraction.include_current_date;

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

        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "registration_number": {"type": "string"},
                    "first_registration_date": {"type": "string", "format": "date"},
                    "vehicle_age_years": {"type": "number"}
                  }
                }
                """;

        byte[] fileBytes = Files.readAllBytes(Path.of("registration.pdf"));
        UploadRequest upload = new UploadRequest("registration.pdf", "application/pdf", fileBytes);

        String guid = client.submitAndProcessDocument(
                upload,
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract vehicle registration details including vehicle age in years")
                        .schema(schemaJson)
                        .includeCurrentDate(true)
                        .build());

        JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.builder().build());
        System.out.println(result.toPrettyString());
    }
}
// docs:end
