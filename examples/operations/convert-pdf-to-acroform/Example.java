package examples.operations.convert_pdf_to_acroform;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.PdfAcroformOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String parentJobGuid = "<job-guid>"; // GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

        JsonNode result = client.submitAndWaitForPdfAcroformOperation(
                parentJobGuid,
                PdfAcroformOptions.builder()
                        .ocr("PREMIUM")
                        .pageRange(List.of(1))
                        .minConfidence(0.6)
                        .maxFieldsPerPage(300)
                        .build(),
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        // The wait returns the operation job GUID and its JSON result; the PDF is a separate download
        String operationJobGuid = result.path("operationJobGuid").asText();
        Files.write(Path.of("fillable.pdf"), client.downloadPdfAcroform(operationJobGuid));

        System.out.println("Operation job GUID: " + operationJobGuid);
        // docs:end
    }
}
