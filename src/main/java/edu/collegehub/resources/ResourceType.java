package edu.collegehub.resources;

public enum ResourceType {
    NOTES("Notes"), QUESTION_PAPER("Previous Question Paper"), PRESENTATION("Presentation"),
    STUDY_GUIDE("Study Guide"), PRACTICE_MATERIAL("Practice Material"), LAB_PRACTICAL_MATERIAL("Lab/Practical Material");
    private final String label;
    ResourceType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
