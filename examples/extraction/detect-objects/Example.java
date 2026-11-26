package examples.extraction.detect_objects;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        Map<String, Object> unitInterval = Map.of("type", "number", "minimum", 0, "maximum", 1);

        Map<String, Object> boundingBox = Map.of(
                "type", "object",
                "description", "Normalized bounding box in image coordinates",
                "properties", Map.of(
                        "left", unitInterval,
                        "top", unitInterval,
                        "right", unitInterval,
                        "bottom", unitInterval),
                "required", List.of("left", "top", "right", "bottom"));

        Map<String, Object> objectProperties = new LinkedHashMap<>();
        objectProperties.put("name", Map.of(
                "type", "string",
                "description", "Short human-readable name, such as ball or player in red jersey"));
        objectProperties.put("object_type", Map.of(
                "type", "string",
                "description", "Category such as player, ball, goal, field_marking, or equipment"));
        objectProperties.put("description", Map.of(
                "type", "string",
                "description", "Brief visual description that helps identify the boxed object"));
        objectProperties.put("bbox", boundingBox);
        objectProperties.put("confidence", unitInterval);

        String schema = Schema.of(Map.of(
                "type", "object",
                "properties", Map.of(
                        "scene_summary", Map.of("type", "string"),
                        "objects", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", objectProperties,
                                        "required", List.of("name", "object_type", "description", "bbox", "confidence")))),
                "required", List.of("scene_summary", "objects")));

        String prompt = """
                Detect the main visible objects in this soccer image.

                Return visible objects only. Include soccer players, the ball, and any prominent
                sports equipment or field markings that are clearly visible. Do not invent
                objects that are hidden, implied, or outside the frame.

                For each object:
                - Use a concise source-image label in name.
                - Set object_type to a short category such as player, ball, goal, field_marking, or equipment.
                - Set bbox to the visible extent of the object only, not a group or surrounding whitespace.
                - Use normalized coordinates from 0 to 1 in image space with origin at the top-left.
                - Set confidence from 0 to 1.

                Return JSON only. The JSON must match the provided schema.""";

        byte[] imageData = Files.readAllBytes(Path.of("soccer.jpg"));

        String jobId = client.submitAndProcessDocument(
                new UploadRequest("soccer.jpg", "image/jpeg", imageData),
                ProcessOptions.builder()
                        .mimeType("image/jpeg")
                        .prompt(prompt)
                        .schema(schema)
                        .ocr("PREMIUM")
                        .llm("DEFAULT")
                        .build());

        JsonNode result = client.waitUntilReadyJson(
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(600))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println(result.toPrettyString());
        // docs:end
    }
}
