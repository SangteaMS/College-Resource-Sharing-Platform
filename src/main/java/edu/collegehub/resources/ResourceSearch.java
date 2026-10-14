package edu.collegehub.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/** Builds database filters for the public resource list. Search words can match different metadata fields. */
final class ResourceSearch {
    private static final Pattern SEMESTER_PHRASE = Pattern.compile("\\b(?:semester|sem)\\s*(?:no\\.?\\s*)?(\\d{1,2})\\b", Pattern.CASE_INSENSITIVE);

    private ResourceSearch() { }

    static Specification<Resource> approved(Long programmeId, Long semesterId, Long subjectId,
                                            ResourceType type, Integer examYear, ExamType examType,
                                            String query, boolean papersOnly) {
        String normalizedQuery = normalize(query);
        Matcher semesterMatcher = SEMESTER_PHRASE.matcher(normalizedQuery);
        Integer phraseSemester = semesterMatcher.find() ? Integer.valueOf(semesterMatcher.group(1)) : null;
        if (phraseSemester != null) {
            normalizedQuery = semesterMatcher.replaceAll(" ");
        }
        final String queryText = normalizedQuery;
        final Integer querySemester = phraseSemester;

        return (root, criteriaQuery, cb) -> {
            var programme = root.join("programme", JoinType.LEFT);
            var semester = root.join("semester", JoinType.LEFT);
            var subject = root.join("subject", JoinType.LEFT);
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.equal(root.get("status"), ModerationStatus.APPROVED));
            filters.add(cb.isFalse(root.get("removed")));
            filters.add(cb.isFalse(root.get("demoResource")));
            if (papersOnly) filters.add(cb.equal(root.get("resourceType"), ResourceType.QUESTION_PAPER));
            if (programmeId != null) filters.add(cb.equal(programme.get("id"), programmeId));
            if (semesterId != null) filters.add(cb.equal(semester.get("id"), semesterId));
            if (subjectId != null) filters.add(cb.equal(subject.get("id"), subjectId));
            if (type != null) filters.add(cb.equal(root.get("resourceType"), type));
            if (examYear != null) filters.add(cb.equal(root.get("examYear"), examYear));
            if (examType != null) filters.add(cb.equal(root.get("examType"), examType));
            if (querySemester != null) filters.add(cb.equal(semester.get("number"), querySemester));

            for (String term : terms(queryText)) {
                String like = "%" + term + "%";
                List<Predicate> matches = new ArrayList<>();
                matches.add(contains(cb, root.get("title"), like));
                matches.add(contains(cb, root.get("description"), like));
                matches.add(contains(cb, root.get("displayFilename"), like));
                matches.add(contains(cb, subject.get("name"), like));
                matches.add(contains(cb, subject.get("code"), like));
                matches.add(contains(cb, programme.get("name"), like));
                matches.add(contains(cb, programme.get("code"), like));
                matches.add(contains(cb, semester.get("number").as(String.class), like));
                matches.add(contains(cb, root.get("examYear").as(String.class), like));
                matches.add(contains(cb, root.get("examSession"), like));
                matches.add(contains(cb, root.get("examType").as(String.class), like));
                matches.add(contains(cb, root.get("resourceType").as(String.class), like));
                matches.add(contains(cb, root.get("fileFormat").as(String.class), like));
                addAliases(term, root, cb, matches);
                filters.add(cb.or(matches.toArray(Predicate[]::new)));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
    }

    private static void addAliases(String term, jakarta.persistence.criteria.Root<Resource> root,
                                   jakarta.persistence.criteria.CriteriaBuilder cb, List<Predicate> matches) {
        switch (term) {
            case "ppt", "slide", "slides" -> {
                matches.add(cb.equal(root.get("fileFormat"), FileFormat.PPTX));
                matches.add(cb.equal(root.get("resourceType"), ResourceType.PRESENTATION));
            }
            case "word" -> matches.add(cb.equal(root.get("fileFormat"), FileFormat.DOCX));
            case "exam", "exams", "question", "questions", "paper", "papers", "previous" ->
                    matches.add(cb.equal(root.get("resourceType"), ResourceType.QUESTION_PAPER));
            case "internal", "midterm" -> matches.add(cb.equal(root.get("examType"), ExamType.MID_TERM));
            case "final", "endterm" -> matches.add(cb.equal(root.get("examType"), ExamType.END_TERM));
            case "class", "quiz", "quizzes", "test" -> matches.add(cb.equal(root.get("examType"), ExamType.CLASS_TEST));
            default -> { }
        }
    }

    private static Predicate contains(jakarta.persistence.criteria.CriteriaBuilder cb, Expression<?> field, String pattern) {
        return cb.like(cb.lower(field.as(String.class)), pattern);
    }

    private static String normalize(String query) {
        if (query == null) return "";
        return query.toLowerCase(Locale.ROOT).replaceAll("mid[-\\s]?term", "midterm")
                .replaceAll("end[-\\s]?term", "endterm").trim();
    }

    private static List<String> terms(String query) {
        return java.util.Arrays.stream(query.split("[^\\p{L}\\p{N}]+"))
                .filter(term -> !term.isBlank() && !term.equals("semester") && !term.equals("sem"))
                .toList();
    }
}
