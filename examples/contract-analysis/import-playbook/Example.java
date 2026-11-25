package examples.contract_analysis.import_playbook;

import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        long caseId = 42L; // id of the case that holds the playbook documents

        // playbook.json holds a playbook in the playbook format
        JsonNode playbook = new ObjectMapper().readTree(Path.of("playbook.json").toFile());

        JsonNode draft = client.getContractAnalysisProfile(caseId, "draft");
        long revision = draft.path("draftRevision").asLong(); // a new playbook is at revision 0

        JsonNode imported = client.importPlaybook(caseId, revision, playbook);
        System.out.println("Draft revision: " + imported.path("draftRevision").asLong());
    }
}
