-- 1. Join resources to their programme, semester, and subject names.
-- Publicly listed resource rule: approved and not removed.
SELECT
    r.title AS resource_title,
    p.name AS programme_name,
    sem.semester_number,
    s.name AS subject_name
FROM resources r
JOIN programmes p ON p.id = r.programme_id
JOIN semesters sem ON sem.id = r.semester_id
JOIN subjects s ON s.id = r.subject_id
WHERE r.status = 'APPROVED'
  AND r.removed = FALSE
ORDER BY p.name, sem.semester_number, s.name, r.title;

-- 2. Filter approved, non-removed resources by a sample keyword
-- in either the resource title or subject name.
SELECT
    r.title AS resource_title,
    p.name AS programme_name,
    sem.semester_number,
    s.name AS subject_name,
    r.resource_type
FROM resources r
JOIN programmes p ON p.id = r.programme_id
JOIN semesters sem ON sem.id = r.semester_id
JOIN subjects s ON s.id = r.subject_id
WHERE r.status = 'APPROVED'
  AND r.removed = FALSE
  AND (LOWER(r.title) LIKE '%database%'
       OR LOWER(s.name) LIKE '%database%')
ORDER BY p.name, sem.semester_number, s.name, r.title;

-- 3. Aggregate approved, non-removed resources by programme and resource type.
SELECT
    p.name AS programme_name,
    r.resource_type,
    COUNT(*) AS resource_count
FROM resources r
JOIN programmes p ON p.id = r.programme_id
WHERE r.status = 'APPROVED'
  AND r.removed = FALSE
GROUP BY p.id, p.name, r.resource_type
ORDER BY p.name, r.resource_type;

-- 4. Count recorded download events by programme, subject, and calendar date.
-- This retains historical events even if a resource is removed later.
SELECT
    p.name AS programme_name,
    s.name AS subject_name,
    DATE(ue.event_at) AS event_date,
    COUNT(*) AS download_event_count
FROM usage_events ue
JOIN resources r ON r.id = ue.resource_id
JOIN programmes p ON p.id = r.programme_id
JOIN subjects s ON s.id = r.subject_id
WHERE ue.event_type = 'DOWNLOAD'
GROUP BY p.id, p.name, s.id, s.name, DATE(ue.event_at)
ORDER BY event_date, p.name, s.name;
