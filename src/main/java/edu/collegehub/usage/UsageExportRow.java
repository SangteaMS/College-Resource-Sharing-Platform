package edu.collegehub.usage;

import java.time.LocalDateTime;

public record UsageExportRow(String eventType, String programmeCode, String subjectCode,
                             String subjectName, int semester, String resourceType,
                             LocalDateTime eventAt) { }
