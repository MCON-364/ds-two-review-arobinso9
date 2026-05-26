package edu.touro.mcon364.finalreview.orderflowhandoff.homework;

import edu.touro.mcon364.finalreview.model.StudentSubmission;
import edu.touro.mcon364.finalreview.model.Submission;
import edu.touro.mcon364.finalreview.model.SubmissionReport;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.counting;

/**
 * Homework 3 — Building a report from a completed collection.
 *
 * A gradebook already contains a list of assignment submissions. Each submission
 * represents one student's work for one assignment. At this point, the data is
 * not changing while the report is being built. Nothing is being produced by one
 * thread and consumed by another thread. We are simply analyzing a collection
 * that already exists.
 *
 * The job of this class is to answer several reporting questions about that
 * collection and then combine those answers into one SubmissionReport.
 *
 * Before coding, think through the shape of the problem:
 * - What information is already available in each StudentSubmission? the grade
 * - Which questions require counting?
 * - Which questions require calculating a numeric summary?
 * - Which questions require grouping submissions by one field?
 * - Which questions require selecting only some submissions?
 * - Since the input list is already complete, do we need threads here?
 *
 * Requirements:
 * - The constructor receives the submissions that will be analyzed.
 * - The builder must not expose or mutate its internal list of submissions.
 * - getLateCount() returns how many submissions were marked late.
 * - getAverageScore() returns the average score across all submissions.
 * - getSubmissionsByAssignment() returns how many submissions exist for each assignment name.
 * - getFailingSubmissions() returns submissions whose score is below 60.
 * - buildReport() returns a SubmissionReport containing all four pieces of information.
 *
 * Edge cases to consider:
 * - An empty submission list should not cause a crash.
 * - A caller should not be able to change this builder's internal state by
 *   modifying the original list after construction.
 * - Returned collections should not allow callers to mutate the builder's
 *   internal state.
 */
public class SubmissionReportBuilder {

    private final List<StudentSubmission> submissions;

    // this validates to make sure that we don't accept nulls- we must do this in the constructor for things of this type
    // meaning - when we are getting a collection from the constructor then use this...If its list -> List.copyOf(). If its a mp, then use Map.copyOf()
    public SubmissionReportBuilder(List<StudentSubmission> submissions) {
        this.submissions = List.copyOf(Objects.requireNonNull(submissions));
    }

    /**
     * Return the number of submissions that were turned in late.
     */
    public long getLateCount() {
        // TODO: answer this reporting question from the submissions collection
        return submissions.stream()
                .filter(StudentSubmission::late)
                .count();
    }

    /**
     * Return the average score across all submissions.
     *
     * If there are no submissions, return 0.0.
     */
    public double getAverageScore() {
        // TODO: answer this reporting question from the submissions collection
        return submissions.stream()
                //extract the score
                .mapToDouble(StudentSubmission::score)
                .average()
                .orElse(0.0);
    }

    /**
     * Return a map where each assignment name is associated with the number of
     * submissions received for that assignment.
     */
    public Map<String, Long> getSubmissionsByAssignment() {
        // TODO: answer this reporting question from the submissions collection

        // To group items by a property (like assignmentName) and count how many items fall into each bucket,
        // you must use Collectors.groupingBy()
        Map<String, Long> groupedMap = submissions.stream()
                .collect(Collectors.groupingBy(
                        StudentSubmission::assignmentName, //key
                        counting() // no semicolon shld be here - value - Counts items per bucket
                        )
                );
        // Makes the map completely unmodifiable before handing it out
        return java.util.Collections.unmodifiableMap(groupedMap);
    }

    /**
     * Return the submissions whose score is below 60.
     */
    // Since the test suite explicitly checks that the list returned by this method cannot be modified from the outside
    // (failingSubmissionsIsUnmodifiable). Using Collectors.toList() returns a standard mutable ArrayList.
    // If someone calls .clear() on it, it won't throw an exception, causing your test to fail.
    // You need to use Collectors.toUnmodifiableList() instead.
    public List<StudentSubmission> getFailingSubmissions() {
        // TODO: answer this reporting question from the submissions collection
        return submissions.stream()
                .filter(studentSubmission -> studentSubmission.score()<60)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Build the complete report by combining the smaller reporting questions.
     */
    public SubmissionReport buildReport() {
        return new SubmissionReport(
                getLateCount(),
                getAverageScore(),
                getSubmissionsByAssignment(),
                getFailingSubmissions()
        );
    }
}

/*
1- Collectors.groupingBy() --> 1-to-Many --> Automatically puts values into a collection bucket (List, Set, or a Count).
--> Grouping multiple items together (e.g., Many submissions under one Homework assignment name).

2- Collectors.toMap() --> 1-to-1 --> Links the key directly to a single, raw object property. --> Building direct lookup registries or ID indexes (e.g., Student Name $\rightarrow$ Score).
We use toMap() when we are certain that every key in our data is unique (or we only want to pick one specific value per key).
If Java finds duplicate keys while using toMap(), it will throw an IllegalStateException unless you explicitly tell
it how to handle the tie.
Code Example: Mapping Student to Score
Let’s say you want a map of student names to their exact score (Map<String, Integer>).
Since each student only has one score in this list, it's a perfect 1-to-1 relationship.

public Map<String, Integer> getStudentScores() {
    return submissions.stream()
            .collect(Collectors.toMap(
                    StudentSubmission::studentName, // Key: The student's name (String)
                    StudentSubmission::score        // Value: Their score (Integer)
            ));
}

What if there are duplicate keys? (The Tie-Breaker)
If your list has two submissions for "Alice", toMap() will panic and crash because it doesn't know which score to keep.
 To fix this, you pass a third argument to toMap() called a merge function. This acts as a tie-breaker.

Keeping the Highest Score:
public Map<String, Integer> getHighestScorePerStudent() {
    return submissions.stream()
            .collect(Collectors.toMap(
                    StudentSubmission::studentName,
                    StudentSubmission::score,
                    (existingScore, newScore) -> Math.max(existingScore, newScore) // Tie-breaker!
            ));
}
 */
