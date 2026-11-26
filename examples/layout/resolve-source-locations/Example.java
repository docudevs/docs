package examples.layout.resolve_source_locations;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String jobGuid = "<job-guid>"; // GUID of a completed job that was processed with sourceLocations enabled

        JsonNode sources = client.getSourceLocations(jobGuid);
        JsonNode layout = client.getDocumentLayout(jobGuid, null, null, List.of("paragraphs"));

        // Print each pointer, its source reference and the paragraph text for that reference.
        // References to tables or figures print an empty string.
        Map<String, String> paragraphText = new HashMap<>();
        for (JsonNode paragraph : layout.path("paragraphs")) {
            paragraphText.put(paragraph.path("id").asText(), paragraph.path("content").asText());
        }
        for (Map.Entry<String, JsonNode> location : sources.path("locations").properties()) {
            for (JsonNode ref : location.getValue().path("sourceRefs")) {
                System.out.println(location.getKey() + " " + ref.asText() + " "
                        + paragraphText.getOrDefault(ref.asText(), ""));
            }
        }
        // docs:end
    }
}
