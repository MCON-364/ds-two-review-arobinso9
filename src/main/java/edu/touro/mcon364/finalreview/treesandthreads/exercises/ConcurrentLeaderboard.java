package edu.touro.mcon364.finalreview.treesandthreads.exercises;

import edu.touro.mcon364.finalreview.treesandthreads.model.ScoreEntry;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

/**
 * In-class Exercise 3 - Concurrent Leaderboard (ConcurrentSkipListSet + ExecutorService)
 *
 * Scenario: a game server receives score submissions from many player threads at
 * the same time. The leaderboard must always reflect current top scores in
 * sorted order (highest first) and must be safe when read and written concurrently.
 *
 * This exercise practises:
 * - ConcurrentSkipListSet as the thread-safe sorted cousin of TreeSet.
 * - Why TreeSet is NOT safe for concurrent access.
 * - ExecutorService and Runnable to simulate concurrent score submissions.
 * - AtomicInteger for a safe submission counter.
 * - Stream operations to produce a ranked snapshot from the sorted set.
 *
 * Before coding, think about:
 * - What would happen if two threads called TreeSet.add() simultaneously?
 * - ConcurrentSkipListSet keeps elements sorted by compareTo.
 *   Look at ScoreEntry.compareTo: which score appears first in iteration?
 * - Each ScoreEntry is unique by (playerName, score, timestamp).
 *   If a player submits a new score, does the old one disappear?
 *
 * Requirements:
 * - submitScore(entry) adds a ScoreEntry thread-safely.
 * - getTopN(n) returns the top n ScoreEntry objects as an immutable list, highest first.
 * - getTotalSubmissions() returns the number of times submitScore has been called.
 * - runSimulation(players, scoresEach) uses an ExecutorService to have each player
 *   submit scoresEach random scores concurrently, then shuts down the pool and waits.
 *
 * Do not use synchronized blocks. Rely on ConcurrentSkipListSet and AtomicInteger.
 */
public class ConcurrentLeaderboard {

    // ScoreEntry.compareTo sorts highest score first
    // ConcurrentSkipListSet is the thread-safe sorted cousin of TreeSet. TreeSet is NOT thread safe!
    private final ConcurrentSkipListSet<ScoreEntry> leaderboard = new ConcurrentSkipListSet<>();
    private final AtomicInteger totalSubmissions = new AtomicInteger(0);

    /**
     * Adds a score entry to the leaderboard thread-safely.
     *
     * @param entry the score entry to add
     */
    public void submitScore(ScoreEntry entry) {
       //TODO
        leaderboard.add(entry);
        totalSubmissions.incrementAndGet();
    }

    /**
     * Returns the top n scores as an immutable list, highest score first.
     *
     * @param n number of top entries to return
     * @return immutable top-n list
     */
    // Because ConcurrentSkipListSet naturally keeps everything perfectly sorted via its internal compareTo logic,
    // your elements are already sitting in the stream from highest score to lowest score.
    // You can delete the sorting lines completely and just go straight to limit(n). --> I deleted the sorted()
    public List<ScoreEntry> getTopN(int n) {
        // TODO
        return leaderboard.stream()
                //  limit(n) Truncates the stream so that only the first n elements (the highest counts) remain.
                .limit(n)
                .toList();
    }

    /**
     * Returns how many times submitScore has been called since creation.
     */
    public int getTotalSubmissions() {
        // TODO
        return totalSubmissions.get();
    }

    /**
     * Simulates concurrent score submissions using an ExecutorService.
     *
     * Each player in the list submits scoresEach amount of random scores on a separate thread.
     * Wait for all threads to finish before returning.
     *
     * @param players    list of player names
     * @param scoresEach number of random scores each player submits
     */
    /*
    In a single-threaded program, you usually use java.util.Random to get random numbers.
    However, in a multi-threaded program, java.util.Random becomes a massive bottleneck.
    Under the hood, java.util.Random uses a single internal number (a seed) to calculate the next random number.
    If 10 threads try to get a random number at the exact same fraction of a second, they will fight over that single seed.
    Java forces them to wait in line (using atomic locks) so they don't corrupt the seed. This fighting is called thread contention.
    ThreadLocalRandom solves this. Instead of one shared generator, it gives every single thread its own private random number generator instance.
     */

    public void runSimulation(List<String> players, int scoresEach)
            throws InterruptedException {
        // we create a pool with one thread per player to maximize concurrency
        ExecutorService executor = Executors.newFixedThreadPool(players.size());
        ThreadLocalRandom random = ThreadLocalRandom.current();


        // we need a nested loop (expressed via streams) to submit multiple scores per player
        for (String player : players) {     // we loop thru the players and grab each player = their name - which we use to create a ScoreEntry later
            // if a player sent in 5 scores then we need to submit 5 diff scores for them...
            for(int i=0; i<scoresEach; i++){
                // When you call executor.submit(), you are dropping a piece of paper into their mailbox.
                // The paper contains a Runnable task—a description of work that needs to be done.
                // We are submitting a lambda expression () -> { ... }.
                // To the executor, this lambda is just a package of code that says: "Hey worker thread, whenever
                // you are free, open this package, generate a random score, and call submitScore for me."
                executor.submit(() -> {
                    // Range: 1 (Inclusive)- to 100,000 (Exclusive)
                    // so 1- 99,999
                    int randomScore = random.nextInt(1, 100000);
                    //get the timeStamp of the score
                    long currentTime = System.currentTimeMillis();

                    //create a ScoreEntry object
                    ScoreEntry entry = new ScoreEntry(player, randomScore, currentTime);
                    // we submit the scoreEntry object
                    submitScore(entry);
                });
            }
        }

        executor.shutdown();
        // Wait up to 10 seconds for all existing submitted tasks to finish running
        executor.awaitTermination(10, TimeUnit.SECONDS);

    }
}
