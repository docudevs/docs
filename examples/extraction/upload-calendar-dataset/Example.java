package examples.extraction.upload_calendar_dataset;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.UploadRequest;
// docs:end
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the knowledge base, from listKnowledgeBases

        byte[] fileData = Files.readAllBytes(Path.of("public-holidays-2024.csv"));
        UploadRequest upload = new UploadRequest("public-holidays-2024.csv", "text/csv", fileData);

        // The Java client throws DocuDevsApiException when the upload is refused
        client.uploadCaseDocument(caseId, upload, "calendar", null);
        System.out.println("Upload accepted");
        // docs:end
    }
}
