package examples.pipelines.correct_ocr_text;

// docs:start
import static ai.docudevs.client.PipelinePath.ocr;

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
                        .sourceLocations(true)
                        .build());

        PipelineNodeRef ocrCorrected = pipelineBuilder.addNodeRef(PipelineNode.ocrCorrect("ocr_corrected")
                .source(ocr().content())
                .mode("correction")
                .scope("pagewise")
                .sourceMap("preserve")
                .llmTier("mini")
                .build());

        PipelineNodeRef extracted = pipelineBuilder.addNodeRef(PipelineNode.extract("extract_corrected")
                .dependsOn(ocrCorrected)
                .source(ocrCorrected.result().content())
                .prompt("Extract fields from the corrected OCR text.")
                .schema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of("anyOf", List.of(
                                        Map.of("type", "string"),
                                        Map.of("type", "null"))))))
                .sourceLocations(true)
                .build());

        PipelineNodeRef finalResult = pipelineBuilder.addNodeRef(PipelineNode.finalResult("final_result")
                .dependsOn(extracted)
                .output(extracted.result())
                .build());

        PipelineDefinition pipeline = pipelineBuilder.finalOrder(finalResult).build();

        System.out.println(pipeline.buildJsonNode().toPrettyString());
        // docs:end
    }
}
