package examples.embedding_providers.create_embedding_provider;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import ai.docudevs.client.EmbeddingProviderOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        // credentialId is the "id" of a shared model credential (see Model credentials).
        long credentialId = 42L;

        JsonNode mutation = client.createEmbeddingProvider(
                "Our OpenAI embeddings",
                "openai",
                "text-embedding-3-small",
                1536,
                EmbeddingProviderOptions.builder()
                        .apiUrl("")
                        .similarityMetric("cosine")
                        .requestOptions(Map.of())
                        .credentialId(credentialId)
                        .setAsDefault(false)
                        .build());
        JsonNode revision = mutation.path("provider").path("latestRevision");
        System.out.println("Revision " + revision.path("id").asLong()
                + ", reindex required: " + mutation.path("reindexRequired").asBoolean());
        // docs:end
    }
}
