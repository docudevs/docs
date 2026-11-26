package examples.pipelines.route_by_document_type;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import static ai.docudevs.client.PipelinePath.ocr;

import ai.docudevs.client.PipelineCondition;
import ai.docudevs.client.PipelineDefinition;
import ai.docudevs.client.PipelineNode;
import ai.docudevs.client.PipelineNodeRef;
import ai.docudevs.client.PipelineOcr;
import ai.docudevs.client.PipelinePath;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
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
        PipelineDefinition.Builder pipelineBuilder = PipelineDefinition.builder()
                .ocr(PipelineOcr.builder()
                        .mode("AUTO")
                        .qualityArtifact(true)
                        .build())
                .maxNodes(2);

        PipelineNodeRef classification = pipelineBuilder.addNodeRef(PipelineNode.extract("classify_document")
                .source(ocr().content())
                .prompt("Classify this document as invoice, safety_data_sheet, or other. Only classify the document.")
                .schema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "document_type", Map.of(
                                        "type", "string",
                                        "enum", List.of("invoice", "safety_data_sheet", "other")),
                                "confidence", Map.of("type", "number"),
                                "reason", Map.of("type", "string")),
                        "required", List.of("document_type", "confidence", "reason")))
                .llmTier("nano")
                .build());

        PipelinePath documentType = classification.result().field("document_type");

        PipelineNodeRef invoice = pipelineBuilder.addNodeRef(PipelineNode.extract("extract_invoice")
                .dependsOn(classification)
                .when(PipelineCondition.eq(documentType, "invoice"))
                .source(ocr().content())
                .prompt("Extract invoice fields from the document. Return null for missing fields.")
                .schema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "invoice_number", Map.of("type", List.of("string", "null")),
                                "vendor_name", Map.of("type", List.of("string", "null")),
                                "total_amount", Map.of("type", List.of("number", "null")),
                                "currency", Map.of("type", List.of("string", "null")))))
                .llmTier("mini")
                .build());

        PipelineNodeRef sds = pipelineBuilder.addNodeRef(PipelineNode.extract("extract_sds")
                .dependsOn(classification)
                .when(PipelineCondition.eq(documentType, "safety_data_sheet"))
                .source(ocr().content())
                .prompt("Extract product and safety information from the document.")
                .schema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "product_name", Map.of("type", List.of("string", "null")),
                                "supplier", Map.of("type", List.of("string", "null")),
                                "signal_word", Map.of("type", List.of("string", "null")))))
                .llmTier("mini")
                .build());

        PipelineNodeRef invoiceFinal = pipelineBuilder.addNodeRef(PipelineNode.finalResult("invoice_final")
                .dependsOn(classification, invoice)
                .when(PipelineCondition.eq(documentType, "invoice"))
                .output(Map.of(
                        "branch", "invoice",
                        "classification", classification.result(),
                        "extracted", invoice.result()))
                .build());

        PipelineNodeRef sdsFinal = pipelineBuilder.addNodeRef(PipelineNode.finalResult("sds_final")
                .dependsOn(classification, sds)
                .when(PipelineCondition.eq(documentType, "safety_data_sheet"))
                .output(Map.of(
                        "branch", "safety_data_sheet",
                        "classification", classification.result(),
                        "extracted", sds.result()))
                .build());

        PipelineNodeRef otherFinal = pipelineBuilder.addNodeRef(PipelineNode.finalResult("other_final")
                .dependsOn(classification)
                .when(PipelineCondition.eq(documentType, "other"))
                .output(Map.of(
                        "branch", "other",
                        "classification", classification.result(),
                        "message", "No specialized extraction branch was selected."))
                .build());

        PipelineDefinition pipeline = pipelineBuilder.finalOrder(invoiceFinal, sdsFinal, otherFinal).build();

        byte[] documentData = Files.readAllBytes(Path.of("report.pdf"));

        String guid = client.processPipelineDocument(
                new UploadRequest("report.pdf", "application/pdf", documentData),
                pipeline,
                "AUTO");

        JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.defaults());
        System.out.println(result.toPrettyString());

        JsonNode nodes = client.getPipelineNodes(guid);
        System.out.println(nodes.toPrettyString());
        // docs:end
    }
}
