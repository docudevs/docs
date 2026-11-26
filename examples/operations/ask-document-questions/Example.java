package examples.operations.ask_document_questions;

import ai.docudevs.client.DocuDevsClient;

// docs:start
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String parentJobGuid = "<job-guid>"; // GUID of a completed document job

        List<String> questions = List.of(
                "What is the contract duration?",
                "Who are the parties involved?",
                "What are the key obligations?");

        for (String question : questions) {
            // One call submits the task and waits for it; it returns the task's result document
            JsonNode answer = client.submitAndWaitForGenerativeTask(
                    parentJobGuid,
                    "Based on this document, answer the following question: " + question,
                    WaitOptions.builder()
                            .timeout(Duration.ofSeconds(120))
                            .pollInterval(Duration.ofSeconds(2))
                            .build());

            System.out.println("Q: " + question);
            System.out.println("A: " + answer.path("generated_text").asText() + "\n");
        }
        // docs:end
    }
}
