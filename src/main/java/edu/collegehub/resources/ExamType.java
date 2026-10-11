package edu.collegehub.resources;

public enum ExamType {
    MID_TERM("Mid-term"), END_TERM("End-term"), OTHER("Other");
    private final String label;
    ExamType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
