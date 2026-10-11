package edu.collegehub.usage;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UsageExportController {
    private final UsageExportService exports;
    public UsageExportController(UsageExportService exports) { this.exports = exports; }

    @GetMapping("/usage/export.csv")
    public ResponseEntity<byte[]> export() {
        StringBuilder csv = new StringBuilder("event_type,programme_code,subject_code,subject_name,semester,resource_type,event_date\n");
        for (UsageExportRow row : exports.rows()) {
            csv.append(cell(row.eventType())).append(',').append(cell(row.programmeCode())).append(',')
                    .append(cell(row.subjectCode())).append(',').append(cell(row.subjectName())).append(',')
                    .append(row.semester()).append(',').append(cell(row.resourceType())).append(',')
                    .append(cell(row.eventAt().toString())).append('\n');
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("collegehub-usage.csv", StandardCharsets.UTF_8).build().toString())
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    private String cell(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
}
