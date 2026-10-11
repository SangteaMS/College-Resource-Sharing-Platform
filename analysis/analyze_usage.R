# CollegeHub usage analysis (base R only).
# Run: Rscript analysis/analyze_usage.R path/to/collegehub-usage.csv [output-folder]
# The exported CSV contains programme and resource usage fields, never student names.

arguments <- commandArgs(trailingOnly = TRUE)
if (length(arguments) < 1) {
  stop("Choose the exported CSV: Rscript analysis/analyze_usage.R collegehub-usage.csv")
}

input_file <- arguments[[1]]
output_folder <- if (length(arguments) >= 2) arguments[[2]] else dirname(input_file)
if (!dir.exists(output_folder)) dir.create(output_folder, recursive = TRUE)

usage <- read.csv(input_file, stringsAsFactors = FALSE, check.names = FALSE)
required_columns <- c("event_type", "programme_code", "subject_code", "subject_name",
                      "semester", "resource_type", "event_date")
missing_columns <- setdiff(required_columns, names(usage))
if (length(missing_columns) > 0) {
  stop(paste("CSV is missing these columns:", paste(missing_columns, collapse = ", ")))
}

if (nrow(usage) == 0) {
  message("The export is valid but contains no usage events yet.")
  quit(save = "no", status = 0)
}

usage$event_date <- as.POSIXct(usage$event_date, format = "%Y-%m-%dT%H:%M:%OS", tz = "UTC")
usage$event_day <- as.Date(usage$event_date, tz = "UTC")

by_programme <- as.data.frame(xtabs(~ programme_code + event_type, data = usage))
by_subject <- as.data.frame(xtabs(~ programme_code + semester + subject_code + subject_name + event_type,
                                  data = usage))
by_day <- as.data.frame(xtabs(~ event_day + event_type, data = usage))

write.csv(by_programme, file.path(output_folder, "usage_by_programme.csv"), row.names = FALSE)
write.csv(by_subject, file.path(output_folder, "usage_by_subject.csv"), row.names = FALSE)
write.csv(by_day, file.path(output_folder, "usage_by_day.csv"), row.names = FALSE)

downloads <- subset(usage, event_type == "DOWNLOAD")
if (nrow(downloads) > 0) {
  subject_downloads <- sort(table(downloads$subject_name), decreasing = TRUE)
  png(file.path(output_folder, "downloads_by_subject.png"), width = 1100, height = 700)
  par(mar = c(10, 5, 3, 1))
  barplot(subject_downloads, las = 2, col = "darkseagreen3",
          main = "Recorded downloads by subject", ylab = "Download events")
  dev.off()
}

message("Analysis files saved to: ", normalizePath(output_folder, winslash = "/", mustWork = TRUE))
