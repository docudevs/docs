package examples.results.download_result_formats;

import ai.docudevs.client.DocuDevsClient;
// docs:start
import com.fasterxml.jackson.databind.JsonNode;
// docs:end
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        String jobGuid = "<job-guid>"; // GUID of a completed extraction job

        // JSON: the stored result as it is, parsed
        JsonNode result = client.fetchResultJson(jobGuid);
        Files.writeString(Path.of("result.json"), result.toPrettyString());

        // CSV: one row per record, nested fields as dotted column names
        byte[] csv = client.fetchResultCsvBytes(jobGuid);
        Files.write(Path.of("result.csv"), csv);

        // Excel: the same table as a workbook
        byte[] workbook = client.fetchResultExcelBytes(jobGuid);
        Files.write(Path.of("result.xlsx"), workbook);
        // docs:end
    }
}
