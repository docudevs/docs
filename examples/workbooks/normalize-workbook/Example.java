package examples.workbooks.normalize_workbook;

import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;

// docs:start
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
// docs:end

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        // docs:start
        byte[] workbookBytes = Files.readAllBytes(Path.of("submission.xlsx"));
        UploadRequest workbook = new UploadRequest(
                "submission.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes);
        ProcessOptions options = ProcessOptions.builder()
                .mimeType(workbook.mimeType())
                .prompt("Find related tables. Preserve source values, units, row grain, and source references.")
                .extractionMode("WORKBOOK")
                .build();

        String guid = client.submitAndProcessDocument(workbook, options);

        JsonNode dataset = client.waitUntilReadyJson(guid, WaitOptions.builder().build());
        System.out.println(dataset);

        // The plan, source map and summary are separate artifacts of the same job
        JsonNode plan = client.getWorkbookPlan(guid);
        JsonNode sourceMap = client.getWorkbookSourceMap(guid);
        JsonNode summary = client.getWorkbookSummary(guid);
        System.out.println(plan);
        System.out.println(sourceMap);
        System.out.println(summary);
        // docs:end
    }
}
