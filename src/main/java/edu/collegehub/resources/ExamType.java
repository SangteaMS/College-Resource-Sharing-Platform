package edu.collegehub.resources;

public enum ExamType {
    MID_TERM("Mid-term / internal"), END_TERM("End-term / final"), CLASS_TEST("Class test / quiz"), OTHER("Other");
    private final String label;
    ExamType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
