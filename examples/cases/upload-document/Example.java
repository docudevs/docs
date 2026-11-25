package examples.cases.upload_document;

import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.UploadRequest;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 123L; // id returned by createCase

        byte[] fileBytes = Files.readAllBytes(Path.of("invoice_001.pdf"));
        UploadRequest upload = new UploadRequest("invoice_001.pdf", "application/pdf", fileBytes);

        client.uploadCaseDocument(caseId, upload);
        System.out.println("Upload status: OK");
        // docs:end
    }
}
