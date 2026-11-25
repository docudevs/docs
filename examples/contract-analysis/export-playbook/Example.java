package examples.contract_analysis.export_playbook;

import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;

// docs:start
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of the case that holds the playbook documents

        JsonNode playbook = client.exportPlaybook(caseId, "draft");

        Files.writeString(Path.of("playbook.json"), playbook.toPrettyString());
        System.out.println("Clauses: " + playbook.path("clauses").size());
        // docs:end
    }
}
