# ShareMats usage analysis (base R only).
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
  subject_downloads <- sort(table(downloads$subject_name), decreasing = FALSE)
  png(file.path(output_folder, "downloads_by_subject.png"), width = 1500, height = 900)
  par(mar = c(5, 14, 4, 2))
  barplot(subject_downloads, horiz = TRUE, las = 1, cex.names = 0.85,
          col = "darkseagreen3", main = "Synthetic example: downloads by subject",
          xlab = "Download events")
  dev.off()
}

daily_activity <- xtabs(~ event_day + event_type, data = usage)
activity_types <- c("UPLOAD", "DOWNLOAD")
for (event_type in activity_types) {
  if (!event_type %in% colnames(daily_activity)) {
    daily_activity <- cbind(daily_activity, setNames(rep(0, nrow(daily_activity)), event_type))
  }
}
daily_activity <- daily_activity[, activity_types, drop = FALSE]
activity_dates <- as.Date(rownames(daily_activity))
png(file.path(output_folder, "daily_activity.png"), width = 1500, height = 800)
par(mar = c(7, 5, 4, 2))
plot(activity_dates, daily_activity[, "UPLOAD"], type = "n", xaxt = "n",
     ylim = range(daily_activity), xlab = "Date", ylab = "Recorded events",
     main = "Synthetic example: daily uploads and downloads")
axis.Date(1, at = seq(min(activity_dates), max(activity_dates), by = "5 days"),
          format = "%b %d", las = 2, cex.axis = 0.9)
grid()
lines(activity_dates, daily_activity[, "UPLOAD"], type = "o", pch = 16, col = "steelblue4")
lines(activity_dates, daily_activity[, "DOWNLOAD"], type = "o", pch = 16, col = "darkgreen")
legend("topright", legend = activity_types, col = c("steelblue4", "darkgreen"),
       lty = 1, pch = 16, bty = "n")
dev.off()

programme_activity <- xtabs(~ programme_code + event_type, data = usage)
for (event_type in activity_types) {
  if (!event_type %in% colnames(programme_activity)) {
    programme_activity <- cbind(programme_activity, setNames(rep(0, nrow(programme_activity)), event_type))
  }
}
programme_activity <- programme_activity[, activity_types, drop = FALSE]
png(file.path(output_folder, "activity_by_programme.png"), width = 1300, height = 800)
par(mar = c(6, 5, 4, 2))
barplot(t(programme_activity), beside = TRUE, names.arg = rownames(programme_activity),
        col = c("steelblue4", "darkgreen"), ylab = "Recorded events",
        xlab = "Programme code", main = "Synthetic example: activity by programme",
        legend.text = activity_types, args.legend = list(x = "topright", bty = "n"))
dev.off()

message("Analysis files saved to: ", normalizePath(output_folder, winslash = "/", mustWork = TRUE))
