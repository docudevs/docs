package examples.lookup_files.manage_configuration_lookup_file;

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
        byte[] lookupData = Files.readAllBytes(Path.of("vendors.csv"));

        // The Java client returns no status code: each call throws DocuDevsApiException unless the API answers 2xx
        client.uploadConfigurationLookupFile(
                "invoice-config", new UploadRequest("vendors.csv", "text/csv", lookupData));
        System.out.println("Uploaded vendors.csv");

        byte[] downloaded = client.downloadConfigurationLookupFile("invoice-config");
        Files.write(Path.of("vendors-downloaded.csv"), downloaded);

        client.deleteConfigurationLookupFile("invoice-config");
        System.out.println("Deleted the configuration lookup file");
        // docs:end
    }
}
