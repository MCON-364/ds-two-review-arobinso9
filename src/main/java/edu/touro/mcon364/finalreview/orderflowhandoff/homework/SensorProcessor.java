package edu.touro.mcon364.finalreview.orderflowhandoff.homework;

import edu.touro.mcon364.finalreview.model.SensorReading;

import java.util.ArrayDeque;
import java.util.DoubleSummaryStatistics;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Homework 2 — Sensor reading processor.
 *
 * * A monitoring system receives readings from sensors over time. One part of the
 *  * program submits readings as they arrive. Another part of the program processes
 *  * those readings using one or more background workers.
 *  * --> multiple threads will be submitting and processing readings at the same time.
 *  *     this is a thread-safety problem, and we need to coordinate the handoff of work between those threads.
 *  * This class is responsible for coordinating that handoff and for keeping a
 *  * summary of the readings that were actually processed.
 *  * --> we need to store the submitted readings somewhere until they are processed,
 *  *     and we need to keep track of the summary statistics for the processed readings.
 *  * The important question is not only "How do we calculate the stats?" It is also:
 *  * "What happens when readings are being submitted and processed by different
 *  * threads at the same time?"
 *
 * Requirements:
 *  * - submit(reading) accepts one new sensor reading for later processing.
 *  *   --> you should put the reading on a thread-safe queue or collection for later processing by the workers.
 *  * - start(workerCount) starts workerCount background workers.
 *  *   --> You should create a thread pool and submit workerCount tasks to it,
 *  * - workerCount must be greater than 0.
 *  *   --> You should validate the input and throw an exception if it's not.
 *  * - Workers should process submitted readings until the processor is stopped and
 *  *   all already-submitted readings have been handled.
 *  *   --> Your worker runnable should check both the running state and the queue state to decide when to exit.
 *  * - stop() tells the processor to stop accepting/processing future work and waits
 *  *   until the workers finish the remaining work.
 *  *   --> You should set a flag to stop accepting new work, interrupt the worker threads to wake them up if they're waiting,
 *  *       and then wait for them to finish.
 *  * - getTotalProcessed() returns how many readings have been processed so far.
 *  *   --> You should use an AtomicInteger to keep track of the total count of processed readings, and return its value here.
 *  * - getStats() returns summary statistics for the processed reading values:
 *  *   count, minimum, maximum, sum, and average.
 *  *   --> You should use a DoubleSummaryStatistics object to keep track of the summary statistics, and return a snapshot of it here.
 *  * - Public reporting methods must not expose mutable internal state.
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
    private final BlockingQueue<SensorReading> queue= new LinkedBlockingQueue<>();

    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    // Instead of you manually writing code to track the highest number, lowest number, total sum, and count,
    // you just "feed" numbers into it using .accept(). Every time you pass a number to .accept(),
    // the object instantly updates its internal records.
    // you can wrap any method in an atomic shell- and then update atomically
    private final AtomicReference<DoubleSummaryStatistics> stats = new AtomicReference<>(new DoubleSummaryStatistics());
    // use an atomic shell
    // we need the flag bc we are going only as the processor is running
    // declare isRunning as false- and then switch it to true when we call start()
    private volatile boolean isRunning= false;
    private ExecutorService executor ;

    public void submit(SensorReading reading) {
        // TODO: decide where submitted readings should be stored
        // Producer- we are making the donuts and dropping them on a tray- the BlockingQueue
        if(isRunning) {
            queue.offer(reading); // or  queue.add(reading);
        }
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


        // TODO: start the requested number of workers
        isRunning = true;
        executor = Executors.newFixedThreadPool(workerCount);

        // Submit the worker loops to run concurrently in the background - getting the workers started! - the consumers
        for (int i = 0; i < workerCount; i++) {
            executor.submit(this::workerLoop);
        }
    }

    /*
    Instead of locking up the whole thread using a heavy synchronized block, updateAndGet uses an optimistic, lock-free approach.
    Here is what happens behind the scenes:
    Snapshotting: The method looks at what is currently saved in the AtomicReference and hands it to your lambda expression as existing.
    Immutability Emulation: Inside the lambda, you do not modify existing. Instead, you treat it as if it were read-only.
    You build a brand new scratchpad (updated), copy the old history over (updated.combine(existing)), and add your new metric (updated.accept(...)).
    The Race Condition Check (Compare-And-Swap): Java tries to replace the old object reference with your new updated reference.
    If no other thread interfered: The swap is successful.
    If another worker snuck in first: The swap fails. Java discards your updated object, grabs the newly modified baseline
    from the other thread, and re-runs your lambda with the fresh data so no numbers are lost!
     */
    private void process(SensorReading reading) {
        // 1. Increment total counter safely
        totalProcessed.incrementAndGet();

        // 2. Perform a Compare-And-Swap (CAS) loop to update statistics atomically
        stats.updateAndGet(existing -> {
            // A. Create a completely brand new, empty stats sheet
            DoubleSummaryStatistics updated = new DoubleSummaryStatistics();

            // B. Copy all existing historical stats into it
            updated.combine(existing);

            // C. Record the new sensor reading's value into it
            updated.accept(reading.value());

            return updated;
        });
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
        while (isRunning || !queue.isEmpty()) {
            try {
                // Wait for a reading to become available, but time out periodically to check the running state
                SensorReading reading = queue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (reading != null) {
                    // Process the reading
                    process(reading);
                }
            } catch (Exception e) {
                // Handle any exceptions that occur during processing
                e.printStackTrace();
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
        if (executor != null) { //if the queue is not empty, then we need to process whats still on the queue in  the main queue
            // reject any brand-new task submittals to the pool - we don't let anyone new into the store.
            // we don't kill the workers threads tho
            executor.shutdown();

            // wait blockingly until all currently running worker loops finish processing the remaining queue
            // these params basically mean - wait till forever. wait till until every single background worker finishes its loop and dies.
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }

        // now we need to drain the queue - we process the remaining messages on the main thread
        // in LogProcessor we validate nulls more...
        SensorReading reading;
        while((reading=queue.poll())!=null) // poll() retrieves and removes the head of the queue, but if the queue is empty, it returns null immediately.
            process(reading);

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
            DoubleSummaryStatistics snapshot = new DoubleSummaryStatistics();
            snapshot.combine(stats.get());
            return snapshot;
    }
}
