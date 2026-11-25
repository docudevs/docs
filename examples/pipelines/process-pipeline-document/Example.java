package examples.pipelines.process_pipeline_document;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import static ai.docudevs.client.PipelinePath.ocr;

import ai.docudevs.client.PipelineDefinition;
import ai.docudevs.client.PipelineNode;
import ai.docudevs.client.PipelineNodeRef;
import ai.docudevs.client.PipelineOcr;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        PipelineDefinition.Builder pipelineBuilder = PipelineDefinition.builder()
                .ocr(PipelineOcr.builder()
                        .mode("AUTO")
                        .sourceLocations(true)
                        .qualityArtifact(true)
                        .build());

        PipelineNodeRef extractData = pipelineBuilder.addNodeRef(PipelineNode.extract("extract_data")
                .source(ocr().content())
                .prompt("Extract the requested fields.")
                .schema(Map.of(
                        "type", "object",
                        "properties", Map.of("name", Map.of("type", "string"))))
                .sourceLocations(true)
                .build());

        PipelineNodeRef finalResult = pipelineBuilder.addNodeRef(PipelineNode.finalResult("final_result")
                .dependsOn(extractData)
                .output(extractData.result())
                .build());

        PipelineDefinition pipeline = pipelineBuilder.finalOrder(finalResult).build();

        byte[] documentData = Files.readAllBytes(Path.of("report.pdf"));

        String guid = client.processPipelineDocument(
                new UploadRequest("report.pdf", "application/pdf", documentData),
                pipeline,
                "AUTO");

        JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.defaults());
        System.out.println(result.toPrettyString());

        // The Java client has no source-location helper; the Python and cURL variants
        // also read the source locations.
        JsonNode nodes = client.getPipelineNodes(guid);
        System.out.println(nodes.toPrettyString());
        // docs:end
    }
}
