package examples.operations.download_acroform_pdf;

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
        String operationJobGuid = "<operation-job-guid>"; // operation job GUID returned when the conversion was submitted

        byte[] pdfBytes = client.downloadPdfAcroform(operationJobGuid);
        Files.write(Path.of("fillable.pdf"), pdfBytes);
        System.out.println("Saved fillable.pdf (" + pdfBytes.length + " bytes)");
        // docs:end
    }
}
