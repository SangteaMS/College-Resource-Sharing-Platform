-- Illustrative demo categories only; these are not verified official curricula.
INSERT INTO programmes (code, name, description) VALUES
('BCA', 'BCA', 'Illustrative demo programme'),
('BCOM', 'B.Com', 'Illustrative demo programme'),
('BBA', 'BBA', 'Illustrative demo programme'),
('BAPOL', 'B.A. Political Science', 'Illustrative demo programme'),
('BSCBIO', 'B.Sc. Biotechnology', 'Illustrative demo programme');

INSERT INTO semesters (programme_id, semester_number)
SELECT p.id, nums.semester_number
FROM programmes p
CROSS JOIN (
    SELECT 1 AS semester_number UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) nums;

INSERT INTO subjects (programme_id, semester_id, code, name)
SELECT p.id, sem.id, 'BCA-JAVA-101', 'Java Programming'
FROM programmes p JOIN semesters sem ON sem.programme_id = p.id AND sem.semester_number = 1
WHERE p.code = 'BCA';
INSERT INTO subjects (programme_id, semester_id, code, name)
SELECT p.id, sem.id, 'BCA-DBMS-201', 'Database Management Systems'
FROM programmes p JOIN semesters sem ON sem.programme_id = p.id AND sem.semester_number = 2
WHERE p.code = 'BCA';
INSERT INTO subjects (programme_id, semester_id, code, name)
SELECT p.id, sem.id, 'BCOM-ACC-101', 'Financial Accounting'
FROM programmes p JOIN semesters sem ON sem.programme_id = p.id AND sem.semester_number = 1
WHERE p.code = 'BCOM';
INSERT INTO subjects (programme_id, semester_id, code, name)
SELECT p.id, sem.id, 'BCOM-ECO-201', 'Business Economics'
FROM programmes p JOIN semesters sem ON sem.programme_id = p.id AND sem.semester_number = 2
WHERE p.code = 'BCOM';
INSERT INTO subjects (programme_id, semester_id, code, name)
SELECT p.id, sem.id, 'BBA-MGMT-101', 'Principles of Management'
FROM programmes p JOIN semesters sem ON sem.programme_id = p.id AND sem.semester_number = 1
WHERE p.code = 'BBA';
INSERT INTO subjects (programme_id, semester_id, code, name)
SELECT p.id, sem.id, 'BBA-MKT-201', 'Marketing Management'
FROM programmes p JOIN semesters sem ON sem.programme_id = p.id AND sem.semester_number = 2
WHERE p.code = 'BBA';
INSERT INTO subjects (programme_id, semester_id, code, name)
SELECT p.id, sem.id, 'BAPOL-POL-101', 'Political Theory'
FROM programmes p JOIN semesters sem ON sem.programme_id = p.id AND sem.semester_number = 1
WHERE p.code = 'BAPOL';
INSERT INTO subjects (programme_id, semester_id, code, name)
SELECT p.id, sem.id, 'BAPOL-GOV-201', 'Indian Government and Politics'
FROM programmes p JOIN semesters sem ON sem.programme_id = p.id AND sem.semester_number = 2
WHERE p.code = 'BAPOL';
INSERT INTO subjects (programme_id, semester_id, code, name)
SELECT p.id, sem.id, 'BSCBIO-CELL-101', 'Cell Biology'
FROM programmes p JOIN semesters sem ON sem.programme_id = p.id AND sem.semester_number = 1
WHERE p.code = 'BSCBIO';
-- Retain the earlier Statistics with R examples as additional BCA demo material;
-- this is not an official curriculum assertion.
INSERT INTO subjects (programme_id, semester_id, code, name)
SELECT p.id, sem.id, 'BCA-STAT-301', 'Statistics with R'
FROM programmes p JOIN semesters sem ON sem.programme_id = p.id AND sem.semester_number = 3
WHERE p.code = 'BCA';
INSERT INTO subjects (programme_id, semester_id, code, name)
SELECT p.id, sem.id, 'BSCBIO-GEN-201', 'Genetics'
FROM programmes p JOIN semesters sem ON sem.programme_id = p.id AND sem.semester_number = 2
WHERE p.code = 'BSCBIO';
