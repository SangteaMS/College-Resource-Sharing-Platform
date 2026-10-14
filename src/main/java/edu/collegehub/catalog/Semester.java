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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "semesters", uniqueConstraints = @UniqueConstraint(columnNames = {"programme_id", "semester_number"}))
public class Semester {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;
    @Column(name = "semester_number", nullable = false)
    private int number;

    protected Semester() { }
    public Semester(Programme programme, int number) {
        this.programme = programme;
        this.number = number;
    }
    public Long getId() { return id; }
    public Programme getProgramme() { return programme; }
    public int getNumber() { return number; }
    public String getLabel() { return "Semester " + number; }
}
