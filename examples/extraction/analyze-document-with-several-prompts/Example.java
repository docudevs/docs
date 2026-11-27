package examples.extraction.analyze_document_with_several_prompts;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] documentData = Files.readAllBytes(Path.of("report.pdf"));

        String ocrJobId = client.uploadDocument(
                new UploadRequest("report.pdf", "application/pdf", documentData));
        client.ocrDocument(
                ocrJobId,
                ProcessOptions.builder().ocr("PREMIUM").mimeType("application/pdf").build(),
                "markdown");
        client.waitUntilReadyText(
                ocrJobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());

        Map<String, String> prompts = new LinkedHashMap<>();
        prompts.put("document_type", "What type of document is this? (invoice, contract, report, form, etc.)");
        prompts.put("key_information", "Extract the most important information: dates, names, amounts, addresses, and key terms.");
        prompts.put("summary", "Provide a concise summary of this document's main content and purpose.");
        prompts.put("tone", "Analyze the tone of this document. Is it formal, informal, positive, negative, urgent?");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode results = mapper.createObjectNode();
        for (Map.Entry<String, String> prompt : prompts.entrySet()) {
            // One call submits the generative task and waits for it; it returns the task's result document
            JsonNode task = client.submitAndWaitForGenerativeTask(
                    ocrJobId,
                    prompt.getValue(),
                    WaitOptions.builder()
                            .timeout(Duration.ofSeconds(120))
                            .pollInterval(Duration.ofSeconds(2))
                            .build());
            results.put(prompt.getKey(), task.path("generated_text").asText());
        }

        System.out.println(results.toPrettyString());
        // docs:end
    }
}
