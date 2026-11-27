package examples.lookup_files.manage_batch_lookup_file;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.UploadRequest;
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
        String batchGuid = "<batch-guid>"; // GUID of a batch whose documents are uploaded and not yet processed

        byte[] lookupData = Files.readAllBytes(Path.of("vendors-2024.csv"));

        // The Java client returns no status code: each call throws DocuDevsApiException unless the API answers 2xx
        client.uploadBatchLookupFile(batchGuid, new UploadRequest("vendors-2024.csv", "text/csv", lookupData));
        System.out.println("Uploaded vendors-2024.csv");

        byte[] downloaded = client.downloadBatchLookupFile(batchGuid);
        Files.write(Path.of("vendors-downloaded.csv"), downloaded);

        client.deleteBatchLookupFile(batchGuid);
        System.out.println("Deleted the batch lookup file");
        // docs:end
    }
}
