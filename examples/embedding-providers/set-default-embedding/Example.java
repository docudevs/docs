package examples.embedding_providers.set_default_embedding;

import ai.docudevs.client.DocuDevsClient;
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
        // revisionId is the revision "id" under provider.latestRevision when the provider was created.
        long revisionId = 12L;

        client.setDefaultEmbeddingBinding(revisionId);

        // Counts of cases on the default revision, on older revisions and on the original index
        JsonNode overview = client.getEmbeddingOverview();
        System.out.println(overview.path("defaultBinding").toPrettyString());
        // docs:end
    }
}
