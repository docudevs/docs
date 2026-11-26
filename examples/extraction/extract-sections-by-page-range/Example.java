package examples.extraction.extract_sections_by_page_range;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] documentData = Files.readAllBytes(Path.of("safety-data-sheet.pdf"));
        UploadRequest upload = new UploadRequest("safety-data-sheet.pdf", "application/pdf", documentData);
        WaitOptions wait = WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build();

        // Upload one document per extraction and process only the pages that hold the section
        // Section 1: Identification (page 1)
        String identificationJob = client.submitAndProcessDocument(upload, ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("Extract product name, manufacturer, and emergency contact.")
                .pageRange(List.of(1))
                .build());
        JsonNode identification = client.waitUntilReadyJson(identificationJob, wait);

        // Section 9: Physical properties (pages 8-9)
        String propertiesJob = client.submitAndProcessDocument(upload, ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("Extract appearance, odor, boiling point, and flash point.")
                .pageRange(List.of(8, 9))
                .build());
        JsonNode properties = client.waitUntilReadyJson(propertiesJob, wait);

        // Sections 14-16: Transport and regulatory (pages 12-14)
        String regulatoryJob = client.submitAndProcessDocument(upload, ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("Extract transport classification and regulatory information.")
                .pageRange(List.of(12, 13, 14))
                .build());
        JsonNode regulatory = client.waitUntilReadyJson(regulatoryJob, wait);

        System.out.println(identification.toPrettyString());
        System.out.println(properties.toPrettyString());
        System.out.println(regulatory.toPrettyString());
        // docs:end
    }
}
