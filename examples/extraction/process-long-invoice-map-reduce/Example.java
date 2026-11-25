package examples.extraction.process_long_invoice_map_reduce;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
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
        byte[] documentData = Files.readAllBytes(Path.of("large-invoice.pdf"));
        UploadRequest upload = new UploadRequest("large-invoice.pdf", "application/pdf", documentData);

        ProcessOptions options = ProcessOptions.builder()
                .prompt("Extract invoice line items (sku, description, quantity, unitPrice, total).")
                .mapReduce(MapReduceOptions.builder()
                        .pagesPerChunk(4)
                        .overlapPages(1)
                        .dedupKey("sku")
                        .parallelProcessing(true)
                        .headerPageLimit(1)
                        .headerIncludeInRows(false)
                        .headerRowPromptAugmentation("This is an invoice from Acme Corp.")
                        .headerSchema("{\"type\": \"object\", \"properties\": {\"invoiceNumber\": {\"type\": \"string\"}, \"issueDate\": {\"type\": \"string\"}, \"billingAddress\": {\"type\": \"string\"}}}")
                        .build())
                .build();

        String jobId = client.submitAndProcessDocument(upload, options);
        System.out.println("Job submitted: " + jobId);

        JsonNode result = client.waitUntilReadyJson(jobId, WaitOptions.defaults());
        System.out.println("Header: " + result.path("header"));
        System.out.println("Extracted " + result.path("records").size() + " line items.");
        // docs:end
    }
}
