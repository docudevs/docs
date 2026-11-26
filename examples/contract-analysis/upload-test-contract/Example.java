package examples.contract_analysis.upload_test_contract;

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
        long caseId = 42L; // id of the case that holds the playbook documents

        byte[] fileBytes = Files.readAllBytes(Path.of("supplier-agreement.pdf"));
        UploadRequest upload = new UploadRequest("supplier-agreement.pdf", "application/pdf", fileBytes);

        client.uploadCaseDocument(caseId, upload, "test_contract", "kept");
        System.out.println("Upload status: OK");
        // docs:end
    }
}
