package edu.touro.mcon364.finalreview.treesandthreads.homework;

import java.util.*;
import java.util.stream.*;

/**
 * Homework 2 - Student GradeBook (TreeMap + Streams + DoubleSummaryStatistics)
 *
 * Scenario: a course has many students. Each student is identified by name and
 * has a numeric grade (0.0 to 100.0). The gradebook must support sorted lookup
 * and statistical analysis.
 *
 * Before coding, think about:
 * - Should the map key be the student name or the grade? Why does it matter? n
 * - What does TreeMap.firstEntry() return? What does lastEntry() return?
 * - How do we turn a numeric score into a letter grade inside a stream?
 *
 * Requirements:
 * - The constructor receives a Map of student name to grade.
 * - buildSortedGradeBook() returns a TreeMap so students are iterated alphabetically.
 * - getStatistics() returns DoubleSummaryStatistics over all grades.
 * - getLetterGradeDistribution() returns a TreeMap counting how many students
 *   received each letter grade: A (90+), B (80-89), C (70-79), D (60-69), F (below 60).
 * - getTopStudents(n) returns the names of the n highest-scoring students, highest first.
 * - getStudentsInScoreRange(low, high) returns a sorted list of student names
 *   whose grade is in [low, high] inclusive.
 *
 * Do not use explicit loops. Use streams and collectors.
 */
public class StudentGradeBook {

    private final Map<String, Double> grades;

    public StudentGradeBook(Map<String, Double> grades) {
        // TODO: validate non-null; store a defensive copy
        if  (grades == null)
            throw new NullPointerException("grades can't be null");
        this.grades = Map.copyOf(grades);
    }

    /**
     * Returns a TreeMap so iteration visits students alphabetically.
     *
     */
    public TreeMap<String, Double> buildSortedGradeBook() {
        // TODO
        // Passing a map directly to a TreeMap constructor sorts it by its keys instantly
        return new TreeMap<>(grades);
    }

    /**
     * Returns summary statistics (count, min, max, average, sum) over all grades.
     *
     */
    public DoubleSummaryStatistics getStatistics() {
        // TODO
        // grades is a Map of name:grade
        return grades.values().stream()
                .mapToDouble(Double::doubleValue) // Unbox to primitive double stream
                .summaryStatistics();
    }

    /**
     * Returns a TreeMap counting students per letter grade.
     *
     */
    public TreeMap<String, Long> getLetterGradeDistribution() {
        // TODO
        // grades is a Map of name:grade
        return grades.values().stream()
                .map(this::toLetterGrade) // Convert number grades to "A", "B", "C"...(Strings)
                .collect(Collectors.groupingBy(
                        letterGrade -> letterGrade, //key
                        TreeMap::new,            // Keeps letter grades sorted ("A" -> "B" -> "C"...)
                        Collectors.counting()    // Counts occurrences per group //value
                ));
    }

    /**
     * Returns the names of the n highest-scoring students, highest first.
     */
    public List<String> getTopStudents(int n) {
        // TODO
        // grades is a Map of name:grade
        // see notes in word freq counter
        return grades.entrySet().stream() // we do entrySet bc we need access to both the key and the value
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) // Sort highest grade first
                .limit(n)
                .map(Map.Entry::getKey) // Extract just the student names
                .toList();
    }

    /**
     * Returns a sorted list of names whose grade falls in [low, high] inclusive.
     *
     */
    public List<String> getStudentsInScoreRange(double low, double high) {
        // TODO
        // grades is a Map of name:grade
        // we do entrySet bc we need access to both the key and the value
        return grades.entrySet().stream()
                .filter(entry -> entry.getValue() >= low && entry.getValue() <= high)
                // Ensure alphabetical sorting by name since source map might be an unordered HashMap
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        /*
        OR: take advantage of the buildSortedGradeBook() method! This is better approach
        return buildSortedGradeBook().entrySet().stream()
                .filter(entry -> entry.getValue() >= low && entry.getValue() <= high)
                .map(Map.Entry::getKey)
                .toList();
         */
    }

    // Helper method to translate numerical scores to letter grades
    private String toLetterGrade(double score) {
        if (score >= 90.0) return "A";
        if (score >= 80.0) return "B";
        if (score >= 70.0) return "C";
        if (score >= 60.0) return "D";
        return "F";
    }
}
