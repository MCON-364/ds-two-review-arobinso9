package edu.touro.mcon364.finalreview.treesandthreads.homework;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.*;

/**
 * Homework 3 - Concurrent Event Log (ConcurrentSkipListMap + ExecutorService)
 *
 * Scenario: a distributed system fires events from many threads at once.
 * Each event has a timestamp (epoch millis) and a message string.
 * The log must store events in time order, be safe for concurrent writes,
 * and support efficient range queries after all threads have finished.
 *
 * This homework practises:
 * - ConcurrentSkipListMap as a thread-safe sorted map (the concurrent TreeMap).
 * - Why we need a unique key strategy when two events arrive at the same
 *   millisecond — use AtomicLong as a tie-breaker.
 * - ExecutorService to simulate concurrent event sources.
 * - Stream operations on the resulting sorted map.
 *
 * Before coding, think about:
 * - What happens if two events share the same Long key in the map? One overwrites the other!
 * - ConcurrentSkipListMap is sorted by Long key. What chronological ordering does that give?
 * - headMap, tailMap, and subMap return live views of the map.
 *   What does "live" mean here, and is that safe after all writers have stopped?
 *
 * Requirements:
 * - logEvent(timestamp, message) records the event. If two events share a
 *   timestamp, both must be stored. Use composite key:
 *     timestamp * 1_000_000L + sequence.getAndIncrement()
 * - runConcurrentSources(sources, eventsEach) uses an ExecutorService to have
 *   each source log eventsEach events, then waits for completion.
 * - getEventsAfter(timestamp) returns all events logged after the given timestamp, in order.
 * - getEventsBetween(from, to) returns events in [from, to] inclusive.
 * - getMostRecentN(n) returns the n most recent events as an immutable list, newest first.
 *
 * Do not use synchronized blocks. Rely on ConcurrentSkipListMap and AtomicLong.
 */
public class ConcurrentEventLog {

    // Thread-safe sorted map: composite Long key -> event message
    private final ConcurrentSkipListMap<Long, String> log = new ConcurrentSkipListMap<>();
    // Used to make keys unique when two events arrive at the same millisecond
    private final AtomicLong sequence = new AtomicLong(0);

    /**
     * Records an event with the given timestamp.
     *
     * Composite key = timestamp * 1_000_000L + sequence.getAndIncrement()
     * This guarantees key uniqueness without disturbing chronological ordering.
     *
     * @param timestamp epoch milliseconds of the event
     * @param message   event description
     */
    public void logEvent(long timestamp, String message) {
        // TODO
        // Build a unique composite key so events at the same millisecond don't overwrite each other
        // Event 1: 777 * 1_000_000L + 0 = 777,000,000  -- this is how we avoid having duplicate keys
        // Event 2: 777 * 1_000_000L + 1 = 777,000,001
        long compositeKey = timestamp * 1_000_000L + sequence.getAndIncrement();
        log.put(compositeKey, message);
    }

    /**
     * Simulates concurrent event sources.
     *
     * Each source name in the list runs in its own thread and calls logEvent
     * eventsEach times (use System.currentTimeMillis() for the timestamp and
     * sourceName + "-" + i as the message).
     *
     * @param sources    list of source names
     * @param eventsEach number of events each source logs
     */
    /*
    When working with lambdas, variables used inside lambdas must be "effectively final" (meaning they cannot change values).
    So, instead of using "i" in the string msg, we need to create a temporary variable inside the loop (int eventId = i;)
     that doesn't change, and pass that to the lambda
     */
    public void runConcurrentSources(List<String> sources, int eventsEach)
            throws InterruptedException {
        // TODO
        ExecutorService executor = Executors.newFixedThreadPool(sources.size());
        //loop thru each list of source names one at a time
        for (String source : sources) {
            // we loop thru each source, eventsEach amount of times
            //for each event, do the following:
            for (int i = 1; i <= eventsEach; i++) {
                //the work each thread needs to do:
                final int eventId = i; // Fixes the "effectively final" lambda restriction- we can't hv vars we are changing inside a lambda
                executor.submit(() -> {
                    long timeStamp = System.currentTimeMillis();
                    String msg= source+ "-"+ eventId;
                    logEvent(timeStamp, msg);
                });
            }
        }
        executor.shutdown();
        // Wait up to 10 seconds for all existing submitted tasks to finish running
        executor.awaitTermination(10, TimeUnit.SECONDS);

    }

    /**
     * Returns all events logged strictly after the given timestamp, in order.
     *
     */
    public List<String> getEventsAfter(long timestamp) {
        // TODO
        // The highest possible composite key for the exact 'timestamp' millisecond
        // ends in 999,999. Adding 1 gives us the perfect inclusive start for anything AFTER it.
        // If timestamp = 777, let's run the math:
        // 777 * 1_000_000L = 777,000,000 (The absolute beginning of millisecond 777).
        // Add 1_000_000L = 778,000,000.
        // Why did we add 1,000,000?
        // Because the maximum possible composite key that could ever be generated during millisecond 777 is 777,999,999
        // (assuming 1 million events didn't hit at the exact same millisecond).
        // By jumping straight to 778,000,000, we are telling the map: "Skip everything that belongs to millisecond 777
        // entirely, and start reading the tree from the absolute first possible instant of millisecond 778 onwards."
        long startKey = (timestamp * 1_000_000L) + 1_000_000L;  // same as this -long startKey = (timestamp + 1) * 1_000_000L;- Move time forward by 1ms, then shift it into a composite key

        // When you call log.tailMap(778000000L, true), the map uses its internal skip-list index to teleport
        // directly to that boundary line. It completely ignores everything above the line and streams only the values below it.
        return log.tailMap(startKey) // default to starting bound inclsuove
                .values() // we are returning the messages
                .stream()
                .toList();
    }

    /**
     * Returns all events in the timestamp range [from, to] inclusive, in order.
     * If we pass in 774,778 -> we will get everything from 774,000,000 to 778,999,999.
     */
    public List<String> getEventsBetween(long from, long to) {
        // TODO
        // Defensive check.
        if (from>to) {
            return List.of();
        }

        // Scale the raw timestamps to match our map's composite key structure
        long startKey = from * 1_000_000L;
        long endKey = to * 1_000_000L + 999_999L; // Captures all sequences inside the 'to' millisecond

        // log.subMap(from, true, to, true): we pass boolean flags here.
        // Setting both to true ensures that both the from timestamp and the to timestamp are inclusive in our slice. so 1-3.999999

        return log.subMap(startKey, true, endKey, true)
                .values()// we are returning the messages
                .stream()
                .toList();
    }

    /**
     * Returns the n most recent events as an immutable list, newest first.
     */
    public List<String> getMostRecentN(int n) {
        // TODO
        // descendingMap() turns the map upside down so newest keys appear first - meaning the most recent ones come first
        return log.descendingMap()
                .values()
                .stream()
                .limit(n)
                .toList();
    }

    /** Returns the total number of logged events. */
    public int size() {
        return log.size();
    }
}

/*

The Solution: The "Composite Key" Trick
To keep everything sorted and unique, we take a single long number and split it into two visual zones:
The Left Side (High Digits): Holds the actual timestamp.
The Right Side (Low Digits): Holds a simple counter (0 to 999,999) that increments if multiple events hit at the exact same millisecond.

Why we Multiply by 1,000,000
Multiplying a number by 1,000,000 simply shifts it over to the left, opening up 6 empty zeros on the right side to act as a placeholder for our counter.
Let's look at what happens when three events hit at millisecond 777:
Event 1: (777 * 1,000,000) + 0 = 777,000,000
Event 2: (777 * 1,000,000) + 1 = 777,000,001
Event 3: (777 * 1,000,000) + 2 = 777,000,002
Because of the multiplication, the keys stay unique, and they are mathematically sorted exactly in the order they occurred!

Why the "Get Events After" Math Works
Now, suppose the requirement says: "Give me all events logged strictly AFTER millisecond 777.
"This means we want to completely skip millisecond 777 and grab everything starting at millisecond 778.
What is the absolute highest key that could ever exist for millisecond 777? It's 777,999,999 (if the counter maxed out).
To find the boundary line where millisecond 778 begins, we run this math:(777 + 1) * 1,000,000 = 778,000,000
When we pass 778,000,000 into log.tailMap(startKey), your tree structure instantly leaps over every single key starting
with 777 and starts reading the data from the very first entry of 778 all the way to the end of time.
 */