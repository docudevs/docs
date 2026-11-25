package examples.quick_start.analyze_document;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] documentData = Files.readAllBytes(Path.of("your-document.pdf"));
        UploadRequest upload = new UploadRequest("your-document.pdf", "application/pdf", documentData);

        // Step 1: OCR the document (the Java client sets the OCR type only, not the output format)
        String ocrGuid = client.ocrDocument(upload, "PREMIUM");
        client.waitUntilReadyJson(ocrGuid, WaitOptions.defaults());

        // Step 2: Generate the analysis
        client.createGenerativeTask(
                ocrGuid,
                "Summarize this document and explain what type of document it is and its main purpose.");

        // The result field holds the task output as a JSON string with a generated_text field
        JsonNode result = client.waitForGenerativeTask(ocrGuid, WaitOptions.defaults());
        System.out.println("AI Analysis:");
        System.out.println(result.path("result"));
        // docs:end
    }
}
