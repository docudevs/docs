package examples.pipelines.bucket_ocr_quality;

// docs:start
import static ai.docudevs.client.PipelinePath.ocr;

import ai.docudevs.client.PipelineBucket;
import ai.docudevs.client.PipelineDefinition;
import ai.docudevs.client.PipelineNode;
import ai.docudevs.client.PipelineNodeRef;
import ai.docudevs.client.PipelineOcr;
import java.util.List;
import java.util.Map;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        // docs:start
        PipelineDefinition.Builder pipelineBuilder = PipelineDefinition.builder()
                .ocr(PipelineOcr.builder()
                        .mode("AUTO")
                        .qualityArtifact(true)
                        .build());

        PipelineNodeRef quality = pipelineBuilder.validateBucket(
                "ocr_quality",
                ocr().quality().score(),
                List.of(PipelineBucket.withMin("good", 0.85), PipelineBucket.named("review")),
                "review");

        PipelineNodeRef extracted = pipelineBuilder.addNodeRef(PipelineNode.extract("extract_data")
                .source(ocr().content())
                .prompt("Extract the requested fields.")
                .schema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of("anyOf", List.of(
                                        Map.of("type", "string"),
                                        Map.of("type", "null"))))))
                .build());

        PipelineNodeRef finalResult = pipelineBuilder.addNodeRef(PipelineNode.finalResult("final_result")
                .dependsOn(quality, extracted)
                .output(Map.of("quality", quality.result(), "extracted", extracted.result()))
                .build());

        PipelineDefinition pipeline = pipelineBuilder.finalOrder(finalResult).build();

        System.out.println(pipeline.buildJsonNode().toPrettyString());
        // docs:end
    }
}
