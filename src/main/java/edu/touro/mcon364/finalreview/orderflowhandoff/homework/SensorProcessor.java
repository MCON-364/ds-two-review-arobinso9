package edu.touro.mcon364.finalreview.orderflowhandoff.homework;

import edu.touro.mcon364.finalreview.model.SensorReading;

import java.util.ArrayDeque;
import java.util.DoubleSummaryStatistics;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Homework 2 — Sensor reading processor.
 *
 * A monitoring system receives readings from sensors over time. One part of the
 * program submits readings as they arrive. Another part of the program processes
 * those readings using one or more background workers.
 *
 * This class is responsible for coordinating that handoff and for keeping a
 * summary of the readings that were actually processed.
 *
 * The important question is not only "How do we calculate the stats?" It is also:
 * "What happens when readings are being submitted and processed by different
 * threads at the same time?" -- so we need volatile boolean flag
 *
 * Requirements:
 * - submit(reading) accepts one new sensor reading for later processing.
 * - start(workerCount) starts workerCount background workers.
 * - workerCount must be greater than 0.
 * - Workers should process submitted readings until the processor is stopped and
 *   all already-submitted readings have been handled.
 * - stop() tells the processor to stop accepting/processing future work and waits
 *   until the workers finish the remaining work.
 * - getTotalProcessed() returns how many readings have been processed so far.
 * - getStats() returns summary statistics for the processed reading values:
 *   count, minimum, maximum, sum, and average.
 * - Public reporting methods must not expose mutable internal state.
 *
 * Before coding, think about:
 * - Which object or objects represent work waiting to be processed?
 * - Which object or objects represent work that has already been processed?
 * - Which state can be accessed by more than one thread?
 * - How will workers know when to keep working and when to stop?
 * - What should happen if getStats() is called while workers are still running?
 * - Is it better to store all processed readings and calculate stats later, or
 *   update numeric summary state as each reading is processed?
 * - If several workers update the same stats, how will those updates stay correct?
 */
public class SensorProcessor {

    /**
     * Accept one sensor reading for processing.
     *
     * @param reading the reading to process later
     */

    // A BlockingQueue natively handles the thread-safe queueing, blocking worker
    // threads when the queue is empty, and unblocking them when new work arrives
    private final BlockingQueue<SensorReading> sensorReadings= new LinkedBlockingQueue<>();

    private final AtomicInteger totalProcessed = new AtomicInteger(0)
            ;
    // Instead of you manually writing code to track the highest number, lowest number, total sum, and count,
    // you just "feed" numbers into it using .accept(). Every time you pass a number to .accept(),
    // the object instantly updates its internal records.
    private final DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
    // lock for modifying the DoubleSummaryStatistics object
    private final Object statsLock = new Object();

    private volatile boolean isRunning= false;
    ExecutorService executor ;

    public void submit(SensorReading reading) {
        // TODO: decide where submitted readings should be stored
        // Producer- we are making the donuts and dropping them on a tray- the BlockingQueue

        // when the shop is closed - we are closed - we are not letting anyone new in.
        // we just finish the orders we are already filling
        if (!isRunning && executor != null) {
            throw new IllegalStateException("Processor has been stopped and is no longer accepting work.");
        }
        sensorReadings.add(reading);
    }

    /**
     * Start background workers that process submitted readings.
     *
     * @param workerCount number of worker threads to start
     * @throws IllegalArgumentException if workerCount is not positive
     */
    public void start(int workerCount) {
        // TODO: validate workerCount
        if (workerCount <= 0)
            throw new IllegalArgumentException("workerCount must be greater than 0");

        // If the shop is already open - then we have all the workers in there... we dont need to add mroe in.
        if (isRunning) {
            throw new IllegalStateException("Processor is already running.");
        }

        // TODO: start the requested number of workers
        isRunning = true;
        executor = Executors.newFixedThreadPool(workerCount);

        // Submit the worker loops to run concurrently in the background - getting the workers started! - the consumers
        for (int i = 0; i < workerCount; i++) {
            executor.submit(this::workerLoop);
        }
    }

    /**
     * Logic run by each worker.
     *
     * This method is private because callers should not run worker logic directly.
     * The worker should repeatedly look for work, process it when available, and
     * eventually exit when the processor is stopping and no work remains.
     */


    private void workerLoop() {
        // the Consumer - the workers sitting by the tray - who grab donuts as they appear,
        // count them, and pack them into boxes.

        // TODO: implement the worker behavior
        // need to pull a SensorReading off the queue, extract its numerical value
        // and feed it into a shared data structure that tracks statistics.

        // keep running while system is active OR while there are leftover items to drain

        while(isRunning ||!sensorReadings.isEmpty()) {
            SensorReading currReading = sensorReadings.poll();
            if (currReading != null) {
                //update the stats with lock
                synchronized (statsLock) {
                    // we use accept() to feed this value into the summaryStats
                    stats.accept(currReading.value());
                }
                // increment the atomic counter
                totalProcessed.incrementAndGet();
            }
        }
    }

    /**
     * Stop the processor and wait for workers to finish.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public void stop() throws InterruptedException {
        // TODO: signal that work should stop
        // we tell workers to stop taking new items once the queue drains
        isRunning = false;
        // TODO: wait for all workers to finish
        if (executor != null) {
            // reject any brand-new task submittals to the pool - we don't let anyone new into the store.
            // we don't kill the workers threads tho
            executor.shutdown();

            // wait blockingly until all currently running worker loops finish processing the remaining queue
            // these params basically mean - wait till forever. wait till until every single background worker finishes its loop and dies.
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Return the number of readings processed so far.
     */
    public int getTotalProcessed() {
        // TODO: return the processed count safely
        return totalProcessed.get();
    }

    /**
     * Return summary statistics for the processed reading values.
     *
     * If no readings have been processed yet, return an empty
     * DoubleSummaryStatistics object.
     */
    /*
    Imagine this.stats is a whiteboard in a busy factory where workers are constantly erasing and updating numbers.
    If a manager wants a report, they don't take the physical whiteboard away from the workers.
    Instead, they take a photo of the whiteboard.
    .combine() is that photo. It takes an empty statistics object (snapshot) and copies all the current values
    (Count, Sum, Min, Max) out of the live this.stats object at that exact microsecond.
    Now, the method can safely return that snapshot to the user. The user can look at the data for
    as long as they want, and even if background workers keep changing this.stats a millisecond later,
    the user's snapshot remains perfectly frozen and safe to read.
     */
    public DoubleSummaryStatistics getStats() {
        // TODO: calculate or return the current statistics safely
        //return a snapshot copy so we do not expose internal mutable state to the caller
        synchronized (statsLock) {
            DoubleSummaryStatistics snapshot = new DoubleSummaryStatistics();
            snapshot.combine(this.stats);
            return snapshot;
        }
    }
}
