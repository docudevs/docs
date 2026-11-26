package examples.templates.acroform_metadata;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.UploadRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
        byte[] pdfBytes = Files.readAllBytes(Path.of("fillable-form.pdf"));

        JsonNode metadata = client.extractAcroformMetadata(
                new UploadRequest("fillable-form.pdf", "application/pdf", pdfBytes));

        JsonNode fields = metadata.path("fields");
        System.out.println("Direct fields: " + fields.size());

        ArrayNode firstFields = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < Math.min(3, fields.size()); i++) {
            firstFields.add(fields.get(i));
        }
        System.out.println(firstFields.toPrettyString());
        // docs:end
    }
}
