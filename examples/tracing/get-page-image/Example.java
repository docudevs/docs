package examples.tracing.get_page_image;

import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String jobGuid = "<job-guid>"; // GUID of a completed job

        // Get the thumbnail of page 0
        byte[] imageBytes = client.downloadImage(jobGuid, 0);
        Files.write(Path.of("page_0.png"), imageBytes);
        // docs:end
    }
}
