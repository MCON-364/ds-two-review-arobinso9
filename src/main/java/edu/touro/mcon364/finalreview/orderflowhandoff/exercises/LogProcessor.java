package edu.touro.mcon364.finalreview.orderflowhandoff.exercises;

import edu.touro.mcon364.finalreview.model.LogLevel;
import edu.touro.mcon364.finalreview.model.LogMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// submit method takes a lambda. Either we can pass a lambda, defined named function, or a method of a class
/**
 * LogProcessor.
 *
 * A server receives log messages from different parts of an application:
 * authentication, payments, reporting, background jobs, and so on. Messages may
 * arrive while earlier messages are still being processed. We want one part of
 * the program to submit log messages, and a small group of worker threads to
 * process those messages in the background.
 *
 * This class represents that log-processing service.
 *
 * The main problem you are solving:
 * - incoming messages need to wait somewhere until a worker is ready for them;
 * - more than one worker may be running at the same time;
 * - every submitted message should be processed once;
 * - while messages are processed, the class must keep accurate summary counts.
 *
 * Requirements:
 * - submit(message) accepts one log message for later processing.
 * - start(workerCount) starts exactly workerCount background workers.
 * - workerCount must be positive.
 * - workers should keep processing while the processor is still accepting work --> so need volatile flag! Volatile accessible to all threads
 *   or while there is still unprocessed work waiting.
 * - stop() tells the processor to stop accepting/expecting more work and waits
 *   until the already-submitted work has been handled.
 * - getTotalProcessed() returns how many log messages have been processed.
 * - getCountsByLevel() returns how many processed messages there were for each
 *   LogLevel.
 * - getCountsByLevel() must not allow callers to mutate this class's internal
 *   state.
 * - The class must behave correctly when multiple threads interact with it.
 *
 * Questions to think about before coding:
 * - Where should the submitted messages wait before a worker processes them?
 * - What behavior do we need from that structure: newest first, oldest first,
 *   priority order, or something else?
 * - Which state is shared by multiple threads?
 * - Which operations must be protected so the statistics stay correct?
 * - How will worker threads know when to continue waiting for work and when to
 *   finish?
 * - What should happen if stop() is called while messages are still waiting?
 * - What should the public getter methods return so outside code cannot damage
 *   the processor's internal state?
 */
public class LogProcessor {

    /*
     * Decide what fields this class needs.
     *
     * Think about:
     * - pending work
     * - worker threads
     * - whether the processor is still running
     * - total processed count
     * - count by log level
     */

    /**
     * Accept one message for processing.
     */

    private final BlockingQueue<LogMessage> queue = new LinkedBlockingQueue<>(); // this can grow- and if messages are being
    // submitted on the queue but nothing is being removed from the queue - then it will fill up- so we make a safeguard-
    // offer() will only add if there is space on the queue.
    private ExecutorService executorService;
    private final AtomicInteger totalProcessed= new  AtomicInteger(0);
    // we use compute() and merge() when working with ConcurrentHashMaps
    // we are tracking how many of each error we have.
    private final ConcurrentHashMap <LogLevel, Integer> countsByLevel = new ConcurrentHashMap<>();
    private volatile boolean running= false; // volatile means everyone can view it at the same time - so all workers can see it

    public void submit(LogMessage message) {
        // TODO: implement
        // ArrayDeque -> use a LinkedBlockingQueue
        // Submit takes in the messages and puts them on a queue.- so use add()
        // then the consumers will take off the queue. The consumers will wait till there is something
        // on the queue b4 doing work

        if(running){
            queue.offer(message); // offer is basically the same as add()
        }
    }

    /**
     * Start the requested number of background workers.
     */
    public void start(int workerCount) {
        // TODO: implement
        if(workerCount<=0){
            throw new  IllegalArgumentException("workerCount must be greater than 0");
        }
        // set running to true bc
        // 1) Accepts work: It unlocks the submit() method so incoming log messages can actually enter the queue.
        // 2) Keeps workers alive: It tells your background worker threads to stay awake and keep waiting for logs to process.
        running=true;

        // creates a new pool and each thread will take a message from the queue
        executorService = Executors.newFixedThreadPool(workerCount);

        for(int i=0; i<workerCount; i++){
            executorService.submit(this::workerLoop);
        }

//        //or could hv used a lambda
//        for(int j=0; j<workerCount; j++){
//            executorService.submit(()->{
//                while(running || !queue.isEmpty()){
//                    try{
//                        process(queue.take());
//                    }
//                    catch(InterruptedException e){
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            });
//        }


    }

    /**
     * The work done by one background worker.
     *
     * You may keep this helper method, rename it, or replace it with another
     * private helper if your design is clearer that way.
     */
    private void workerLoop() {
        // TODO: implement
        try{
            while(running || !queue.isEmpty()){
                process(queue.take()); // take() retrieves and removes the head of the queue, but if the queue is empty, it completely freezes execution on that thread and waits until an item becomes available.
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process one message and update whatever statistics this class tracks.
     */
    private void process(LogMessage message) {
        // TODO: implement
        totalProcessed.incrementAndGet();
        // update counts by level atomically
        // merge is Atomic op on concurrent hashmap. It takes 3 params- key, value to apply to the method, and a method.
        // So here - we will take the current value, and then every time we get another value, we will add one.
        countsByLevel.merge(message.level(), 1, Integer::sum);

        /*
        Using compute() to update counts atomically
        If the log level hasn't been seen yet, currentCount will be null.
        We initialize it to 1, otherwise we increment the existing count.
        Is key (LogLevel) present? If yes -> currentCount = X. If no -> currentCount = null
        Then we Execute lambda: (key, val) ->  Returns: X + 1  OR  Returns: 1

        countsByLevel.compute(message.level(), (key, currentCount) -> {
        return (currentCount == null) ? 1 : currentCount + 1;
    });

        merge()	-> Provide a default value + an aggregation function. ->	Only executes if a value already exists.
        params: 1) key:	LogLevel -> The key you want to look up or insert.
                2) value: Integer(1) -> The default value to use if the key does not exist yet.
                3) remappingFunction: BiFunction (Integer::sum) ->	The formula to combine the old value and the new value if the key does exist. It receives (oldValue, newValue).

        compute() ->	Compute a new value based entirely on the key and the current value. ->	Always executes and must manually check for null.
        params: 1) key: LogLevel -> The key you want to look up or insert.
                2) remappingFunction: BiFunction -> A formula that calculates the final value. It runs regardless of whether the key exists or not. It receives (key, currentValue).
         */
    }

    /**
     * Stop the processor and wait for worker threads to finish.
     */
    public void stop() throws InterruptedException {
        // TODO: implement
        // set running to false bc:
        // 1) Locks the door: It forces submit() to reject any new logs, ensuring no new work piles up while you are trying to clean up.
        // 2) Initiates closing shift: It signals the worker threads that no new work is coming.
        // They know they just need to finish whatever is currently left in the queue, and then they can safely shut down.
        running=false;

        // handles the edge case where someone accidentally calls stop() before ever calling start() so no NPE will b thrown by shutdown
        if(executorService==null)
            return;

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        // drain any messages that were in the queue but not yet picked up
        // we call messages in the queue and process them on the main thread - since we already shut down the worker threads above
        // this is how we make sure we processed all the remaining messages on the queue - so now we did :)
        LogMessage msg;
        while((msg=queue.poll())!=null) // poll() retrieves and removes the head of the queue, but if the queue is empty, it returns null immediately.
            process(msg);
    }

    /**
     * Return the number of messages processed so far.
     */
    public int getTotalProcessed() {
        // TODO: implement
        return totalProcessed.get();
    }

    /**
     * Return a safe snapshot of the counts by level.
     */
    public Map<LogLevel, Integer> getCountsByLevel() {
        // TODO: implement
        return Map.copyOf(countsByLevel);
    }
}
