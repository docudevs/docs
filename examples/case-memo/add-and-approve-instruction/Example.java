package examples.case_memo.add_and_approve_instruction;

// docs:start
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        // docs:start
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        long caseId = 42L; // id of a records case

        JsonNode entry = client.addMemoEntry(
                caseId,
                "instruction",
                Map.of("kind", "subtree", "code", "7.31"),
                "Fiction books are filed by genre when the description names one.");
        System.out.println("Proposed entry " + entry.path("id").asText());

        // Nothing changes in classification until the instruction is approved
        JsonNode approved = client.approveMemoEntry(caseId, entry.path("id").asText());
        System.out.println(approved.path("id").asText() + " " + approved.path("status").asText()
                + " " + approved.path("memoVersion").asText());
        // docs:end
    }
}
