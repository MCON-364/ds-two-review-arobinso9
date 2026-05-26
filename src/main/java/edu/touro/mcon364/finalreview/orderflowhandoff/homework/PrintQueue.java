package edu.touro.mcon364.finalreview.orderflowhandoff.homework;

import edu.touro.mcon364.finalreview.model.PrintJob;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.concurrent.Semaphore;

/**
 * Homework 1 — PrintQueue.
 *
 * A small print room has one shared printer. Many print jobs can be submitted,
 * but the printer can only work on one job at a time.--use a semaphore
 *
 * The print room should behave the way people expect a normal printer line to
 * behave: jobs wait until it is their turn, and the next job selected for
 * printing should be based on the order in which jobs arrived. --use a Queue
 *
 * This class is responsible for remembering the waiting print jobs and exposing
 * the operations that the rest of the program would need:
 * submitting a new job, printing the next job, checking what would print next,
 * and reporting how many jobs are still waiting.
 *
 * Before coding, think through the shape of the problem:
 * - What information does this object need to remember between method calls?
 * - When a new job is submitted, where should it be placed? end of the queue
 * - When the printer is ready, which job should be selected? front of queue. FIFO
 * - Is this problem about the most recent item, the oldest item, or all items? oldest item
 * - Which collection behavior matches that rule? queue
 * - What should the methods return when there are no waiting jobs?
 *
 * Requirements:
 * - submit(job) records a new print job as waiting.
 * - printNext() removes and returns the job that should be printed next.
 * - printNext() returns Optional.empty() if no jobs are waiting.
 * - peekNext() returns the job that would be printed next, but does not remove it.
 * - peekNext() returns Optional.empty() if no jobs are waiting.
 * - queuedJobs() returns the number of print jobs currently waiting.
 * - The class should not expose its internal collection directly.
 */
public class PrintQueue {

    // TODO: choose the field or fields needed to remember waiting print jobs
    //queue of jobs
    ArrayDeque <PrintJob> jobs = new ArrayDeque<>();

    /**
     * Records a new print job as waiting.
     *
     * @param job the print job to add
     */
    public void submit(PrintJob job) {
        // TODO: implement
        jobs.add(job);
    }

    /**
     * Removes and returns the print job that should be handled next.
     *
     * @return the next print job, or Optional.empty() when no jobs are waiting
     */
    //In an ArrayDeque, pop() acts like a stack LIFO, so use poll() and removeFirst() instead for FIFO
    // calling Optional.of() on a variable that could potentially be null is unsafe.
    // Java will crash with a NullPointerException instead of returning Optional.empty().
    // To be safe, you should use Optional.ofNullable()
    public Optional<PrintJob> printNext() {
        // TODO: implement
        if (jobs.isEmpty())
            return Optional.empty();
        else{
            PrintJob job= jobs.removeFirst();
            // we need to wrap job in an Optional to match return type
            return Optional.ofNullable(job);

        }
    }

    /**
     * Returns the print job that would be handled next without removing it.
     *
     * @return the next print job, or Optional.empty() when no jobs are waiting
     */
    public Optional<PrintJob> peekNext() {
        // TODO: implement
        if (jobs.isEmpty())
            return Optional.empty();
        else{
            PrintJob job= jobs.peek();
            return Optional.ofNullable(job);
        }
    }

    /**
     * Returns the number of jobs currently waiting to be printed.
     */
    public int queuedJobs() {
        // TODO: implement
        return jobs.size();
    }
}
