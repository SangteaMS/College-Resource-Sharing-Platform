ALTER TABLE subjects ADD COLUMN normalized_name VARCHAR(150) NULL;

UPDATE subjects
SET normalized_name = LOWER(TRIM(name));

ALTER TABLE subjects MODIFY COLUMN normalized_name VARCHAR(150) NOT NULL;

CREATE UNIQUE INDEX uq_subject_programme_semester_normalized_name
    ON subjects (programme_id, semester_id, normalized_name);
