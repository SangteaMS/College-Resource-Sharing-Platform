package edu.collegehub.resources;

public record ResourceItem(
        String id,
        String title,
        String subject,
        String semester,
        String type,
        String description,
        String sampleText) {
}
