package examples.case_memo.read_and_retire_entries;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.MemoListOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        long caseId = 42L; // id of a records case
        String entryId = "m_88"; // id returned when the entry was created

        JsonNode memo = client.listMemo(caseId, MemoListOptions.builder().status("active").build());
        for (JsonNode entry : memo.path("items")) {
            System.out.println(entry.path("id").asText() + " " + entry.path("type").asText()
                    + " " + entry.path("status").asText());
        }

        JsonNode history = client.listMemoRevisions(caseId, entryId);
        for (JsonNode revision : history.path("items")) {
            System.out.println("revision " + revision.path("revision").asText() + ": " + revision.path("status").asText());
        }

        // Retiring adds a revision; earlier versions stay readable
        JsonNode retired = client.retireMemoEntry(caseId, entryId);
        System.out.println(retired.path("id").asText() + " " + retired.path("status").asText()
                + " " + retired.path("memoVersion").asText());
        // docs:end
    }
}
