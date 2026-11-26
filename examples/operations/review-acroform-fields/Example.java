package examples.operations.review_acroform_fields;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String parentJobGuid = "<job-guid>"; // GUID of the completed PDF job that was converted
        String operationJobGuid = "<operation-job-guid>"; // operation job GUID returned by the conversion

        ObjectMapper mapper = new ObjectMapper();

        JsonNode generatedMetadata = client.getAcroformMetadata(operationJobGuid);
        JsonNode definitionsArtifact = client.getPdfAcroformFieldDefinitions(operationJobGuid);

        ArrayNode firstMetadataFields = mapper.createArrayNode();
        ArrayNode firstDefinitions = mapper.createArrayNode();
        generatedMetadata.path("fields").forEach(field -> {
            if (firstMetadataFields.size() < 3) {
                firstMetadataFields.add(field);
            }
        });
        definitionsArtifact.path("fieldDefinitions").forEach(definition -> {
            if (firstDefinitions.size() < 3) {
                firstDefinitions.add(definition);
            }
        });
        System.out.println(firstMetadataFields.toPrettyString());
        System.out.println(firstDefinitions.toPrettyString());

        // Mark every generated field as reviewed and accepted
        List<Map<String, Object>> reviewedFields = new ArrayList<>();
        for (JsonNode definition : definitionsArtifact.path("fieldDefinitions")) {
            Map<String, Object> field = mapper.convertValue(definition, new TypeReference<Map<String, Object>>() { });
            field.put("reviewStatus", "accepted");
            reviewedFields.add(field);
        }

        JsonNode applied = client.submitAndWaitForPdfAcroformApplyOperation(
                parentJobGuid,
                reviewedFields,
                operationJobGuid,
                0.0,
                300,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        // The wait returns the operation job GUID and its JSON result; the PDF is a separate download
        String appliedJobGuid = applied.path("operationJobGuid").asText();
        Files.write(Path.of("reviewed.pdf"), client.downloadPdfAcroform(appliedJobGuid));

        System.out.println("Operation job GUID: " + appliedJobGuid);
        // docs:end
    }
}
