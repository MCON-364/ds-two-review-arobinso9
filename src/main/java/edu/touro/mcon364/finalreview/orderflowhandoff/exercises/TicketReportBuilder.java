package edu.touro.mcon364.finalreview.orderflowhandoff.exercises;

import edu.touro.mcon364.finalreview.model.Priority;
import edu.touro.mcon364.finalreview.model.SupportTicket;
import edu.touro.mcon364.finalreview.model.TicketReport;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Building a report from completed work.
 *
 * A support system stores tickets after they have been submitted and worked on.
 * Each ticket has information such as its category, priority, whether it was
 * resolved, and how many minutes it took to resolve.
 *
 * The goal of this class is to turn a list of individual tickets into one
 * summary report. We are not modifying tickets. We are looking across the
 * collection and answering questions about the data.
 * -
 * - The tickets are already available as a collection.
 * - The job is to analyze the collection and produce answers.
 *
 * Before coding, think about the problem in two layers:
 *
 * Layer 1 — What data does this object need?
 * - Should the list of tickets be passed into every method? yes and use streams
 * - Or does it make sense for the builder to receive the list once and then
 *   answer several report questions about that same list? no
 * - If this class stores the list, should it keep the original reference or
 *   protect itself with a copy? yes
 *
 * Layer 2 — What questions does the report ask?
 * - Which questions produce a single number?
 * - Which questions produce a map?
 * - Which questions produce a smaller list?
 * - Which questions require looking only at resolved tickets?
 * - Which questions require looking only at unresolved tickets?
 *
 * Requirements:
 * - The constructor receives the tickets to analyze.
 * - The original list must not be modified by this class.
 * - getResolvedCount() returns how many tickets have been resolved.
 * - getAverageResolutionMinutes() returns the average resolution time for
 *   resolved tickets only.
 * - getCountByCategory() returns how many tickets belong to each category.
 * - getHighPriorityUnresolved() returns unresolved tickets that should receive
 *   the most urgent attention.
 * - buildReport() combines the answers from the smaller methods into one
 *   TicketReport object.
 * - Use streams to express the data processing logic.
 * - Do not use loops.
 *
 * Edge cases to think about:
 * - What should the constructor do if the provided list is null?
 * - What should the average be if there are no resolved tickets?
 * - What should the category-count map look like if the ticket list is empty?
 * - Should callers be able to modify the list returned by
 *   getHighPriorityUnresolved()?
 * - Should callers be able to modify the map returned by getCountByCategory()?
 */
public class TicketReportBuilder {

    private final List<SupportTicket> tickets;

    /**
     * Store the tickets that this report builder will analyze.
     *
     * Think carefully about whether this constructor should keep the original
     * list reference or store a defensive copy.
     */
    public TicketReportBuilder(List<SupportTicket> tickets) {
        // TODO: validate and store the tickets this object will analyze
        if (tickets == null) {
            throw new IllegalArgumentException("tickets cannot be null");
        }
        this.tickets = List.copyOf(tickets);
    }

    /**
     * Return how many tickets in this report data set were resolved.
     */
    public long getResolvedCount() {
        // TODO: calculate from tickets

        return tickets.stream()
                .filter(ticket -> ticket.resolved()==true)
                .count();
    }

    /**
     * Return the average resolution time for resolved tickets only.
     *
     * Tickets that are not resolved should not affect this average.
     */
    public double getAverageResolutionMinutes() {
        // TODO: calculate from tickets- I could hv used summaryStats instead
        return tickets.stream()
                .filter(ticket -> ticket.resolved()==true)
                // .mapToInt(...): This tells the stream, "I want to transform every item inside this stream into a primitive Java int."
                // It converts your stream of complex objects (Stream<SupportTicket>) into a specialized stream of primitive integers (IntStream).
                // In Java, standard streams handle generic objects (like Stream<SupportTicket>).
                // If you try to calculate an average directly on an object stream, Java doesn't know how—because you can't mathematically add or divide custom objects together.
                // By using mapToInt, you switch to a highly optimized IntStream that unlocks built-in statistical functions like:
                //.average(), .sum(), .max(), .min()
                // SupportTicket::minutesToResolve: This is a method reference. It is a cleaner, shorthand way of writing the lambda expression: ticket -> ticket.minutesToResolve().
                // So in one single step, Java does two things at the exact same time:
                // Extraction (Pulling): It reaches inside the SupportTicket object and grabs the value of minutesToResolve.
                // Conversion (Casting): It unwraps that value from the object structure and drops it directly into a primitive IntStream.
                // If you want to extract the minutes without converting them into primitive values, you use the standard .map() method instead of .mapToInt()
                .mapToInt(SupportTicket::minutesToResolve)
                .average()
                .orElse(0);
        /* Or using Summary Statistics:
        return tickets.stream()
            .filter(SupportTicket::resolved)
            .mapToInt(SupportTicket::minutesToResolve)
            .summaryStatistics() // ◄ Gathers all math stats at once
            .getAverage();       // ◄ Extracts just the average
        Metric Available	How to grab it from your code	Value if stream is empty
        Average	                .getAverage()	             0.0
        Count	                .getCount()	                 0
        Sum	                    .getSum()	                 0
        Maximum	                .getMax()	                 Integer.MIN_VALUE
        Minimum	                .getMin()	                 Integer.MAX_VALUE
         */
    }

    /**
     * Return how many tickets belong to each category.
     */
    public Map<String, Long> getCountByCategory() {
        // TODO: calculate from tickets
        Map<String, Long> mutableMap= tickets.stream()
                .collect(Collectors.groupingBy(
                        ticket-> ticket.category(), //key- or use SupportTicket::category
                                Collectors.counting() //value
                ));
        // we return a non modifiable map so now no one can destroy or wipe our data:)
        return Collections.unmodifiableMap(mutableMap);
    }

    /**
     * Return unresolved tickets that should receive the most urgent attention.
     */
    public List<SupportTicket> getHighPriorityUnresolved() {
        // TODO: calculate from tickets
        return tickets.stream()
                .filter(ticket -> ticket.resolved()== false && ticket.priority() == Priority.HIGH)
                .toList();
    }

    /**
     * Build one summary report by combining the smaller report questions above.
     */
    public TicketReport buildReport() {
        return new TicketReport(
                getResolvedCount(),
                getAverageResolutionMinutes(),
                getCountByCategory(),
                getHighPriorityUnresolved()
        );
    }
}
