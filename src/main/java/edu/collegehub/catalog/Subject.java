package edu.collegehub.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Locale;

@Entity
@Table(name = "subjects")
public class Subject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "normalized_name", nullable = false, length = 150)
    private String normalizedName;
    @Column(nullable = false)
    private boolean active = true;

    protected Subject() { }
    public Subject(Programme programme, Semester semester, String code, String name) {
        this.programme = programme;
        this.semester = semester;
        this.code = code;
        this.name = name.trim();
        this.normalizedName = normalize(this.name);
    }
    public static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
    public Long getId() { return id; }
    public Programme getProgramme() { return programme; }
    public Semester getSemester() { return semester; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public boolean isActive() { return active; }
}
