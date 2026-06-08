package edu.touro.mcon364.finalreview.treesandthreads.exercises;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * In-class Exercise 1 - Word Frequency Counter (TreeMap + Streams)
 *
 * Scenario: you receive a list of words (already lowercased and cleaned).
 * You need to count how many times each word appears and then answer
 * several questions about those counts - all in sorted order.
 *
 * This exercise practises:
 * - Why TreeMap gives us sorted-key iteration for free.
 * - How Collectors.groupingBy + Collectors.counting builds a frequency map.
 * - How NavigableMap operations (firstKey, lastKey, headMap, tailMap) let us
 *   slice the sorted map without iterating manually.
 * - How a stream pipeline can rank or filter the frequency entries.
 *
 * Before coding, think about:
 * - If we use HashMap instead of TreeMap, which methods would break, and why?
 * - What is the difference between headMap(key) and headMap(key, true)?
 * - Should getTopN return words with the highest count or the lowest count?
 *
 * Requirements:
 * - The constructor receives the list of words to analyze.
 * - buildFrequencyMap() returns a TreeMap<String, Long> where every key is a
 *   unique word and every value is how many times that word appeared.
 * - getTopN(n) returns the n words with the highest frequency, sorted
 *   descending by count. Ties may appear in any order.
 *   Note that you have to sort the frequency map by value, not by key, to get the top N.
 * - getWordsStartingWith(prefix) returns a sorted list of all words whose
 *   first character equals the given prefix character (e.g., 'a').
 * - getMostFrequentInRange(from, to) returns the word with the highest count
 *   among words in the alphabetical range [from, to] inclusive.
 *   Return Optional.empty() if the range is empty.
 *
 * Do not use explicit loops anywhere. Use streams and collectors instead.
 */
public class WordFrequencyCounter {

    private final List<String> words;

    public WordFrequencyCounter(List<String> words) {
        // TODO: validate that words is not null
        // TODO: store a defensive copy so outside code cannot mutate this object
        if  (words == null)
            throw new IllegalArgumentException("words cannot be null or empty");

        this.words = List.copyOf(words);
    }

    /**
     * Counts how many times each word appears.
     * The returned map must be sorted alphabetically by word.
     * @return sorted frequency map
     */
    // Collectors.toCollection() is used for gathering elements into a Collection (like a TreeSet).
    // To build a Map, you need Collectors.groupingBy() for 1:M or Collectors.toMap() for 1:1
    // Instead, we use Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()).
    // This groups by the word, ensures the factory produces a TreeMap, and counts occurrences.
    public TreeMap<String, Long> buildFrequencyMap() {
        // TODO
        return words.stream()
                .collect(Collectors.groupingBy( // we create a map
                        Function.identity(), //use the word itself as the key
                        TreeMap::new, // groupingBy by default creates a HashMap, so here we are creating a TreeMap instead
                        Collectors.counting() // downstream collector. counts how many times each word appears.
                ));
    }

    /**
     * Returns the n most frequent words, highest count first.
     *
     * @param n number of top words to return -- its in descending order so we want the first 5 - we have highest frequencys first
     * @return list of words, most frequent first
     */
    public List<String> getTopN(int n) {
        // TODO
        // We pull the sorted frequency map we just built, grab its entry set (key-value pairs), and stream it.
        // so we are working w a sorted TreeMap of pairs of: (word: # of appearances)
        // We must stream the entries -entrySet() bc we need access to both the key and the value
        // Now we need to sort by the values (the counts), not the keys- words
        return buildFrequencyMap().entrySet().stream()
                // Map.Entry.comparingByValue() tells the stream to sort the pairs by their counts.
                // Comparator.reverseOrder() flips it so that the largest counts come first. (bc normally its in ascending order. we need descending)
                // so we sort by comparing the values in descending order.
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                //  limit(n) Truncates the stream so that only the first n elements (the highest counts) remain.
                .limit(n)
                // Since the method signature expects a List<String>, we extract just the word (the key) from the entry pair and discard the count.
                .map(Map.Entry::getKey)
                // Collects the final stream of strings into an immutable list.
                .toList();
    }

    /**
     * Returns all words whose first letter equals the given prefix letter,
     * in alphabetical order.
     *
     * @param prefix the starting letter (e.g., 'b')
     * @return sorted list of matching words
     */
    public List<String> getWordsStartingWith(char prefix) {
        // TODO
        // Converts the character (e.g., 'b') into a string ("b").
        String start = String.valueOf(prefix);
        // Increments the character character-code wise (e.g., 'b' becomes 'c') and converts it to a string ("c")
        String end = String.valueOf((char) (prefix + 1));

        // buildFrequencyMap().subMap(start, end): This leverages a specialized method on NavigableMap (which TreeMap implements). subMap(fromKey, toKey)
        // takes advantage of the underlying red-black tree layout to instantly isolate a chunk of the map. By default, the start boundary
        // is inclusive and the end boundary is exclusive.
        // Example: .subMap("b", "c") fetches every word starting with "b", up to but excluding "c".
        // .keySet(): Extracts just the sorted words from that specific slice.
        // Why .keySet(): We only care about the words themselves (the keys), not how many times they appeared (the values).
        // new ArrayList<>(...): Wraps the resulting set view into a standard array list to match the return type requirement.
        return new ArrayList<>(buildFrequencyMap().subMap(start, end).keySet());
    }

    /**
     * Finds the most frequent word in the alphabetical range [from, to] inclusive.
     *
     *
     * @param from lower bound word (inclusive)
     * @param to   upper bound word (inclusive)
     * @return Optional containing the most frequent word in range, or empty if none
     */
    /*
    1. Stream .map()
    Applies to: A collection of many elements inside an open stream.
    Purpose: Transforms each individual item in the stream as it passes through the pipeline.
    Output: A new Stream<T> containing the transformed elements.

    2. Optional .map()
    Applies to: A single container object that is either holding one value or is empty (which is what .max() returns to prevent crashing if the stream was empty).
    Purpose: If a value is present inside the container, transform it. If the container is empty, do nothing and just stay empty.

    Output: A new Optional<T> containing the transformed internal value (or an empty Optional).
     */
    public Optional<String> getMostFrequentInRange(String from, String to) {
        // TODO
        TreeMap<String, Long> freqMap = buildFrequencyMap();

        // Defensive check. If a user queries an invalid range where the starting word is alphabetically greater than the ending word
        // (e.g., from = "zoo", to = "apple"), it immediately returns Optional.empty()
        if (from.compareTo(to) > 0) {
            return Optional.empty();
        }
        // freqMap.subMap(from, true, to, true): Unlike the previous method, we pass boolean flags here.
        // Setting both to true ensures that both the from word and the to word are inclusive in our slice.
        return freqMap.subMap(from, true, to, true)
                // Converts the isolated range slice into a stream of entry pairs.
                .entrySet() // we must specify the set when we are working with TreeMaps!
                .stream()
                // The terminal .max() operation scans the stream and finds the entry with the highest numeric value (the frequency). bc we did comparingByValue
                // Because it evaluates the stream, it returns an Optional<Map.Entry<String, Long>> (handling cases where the range might be empty).
                .max(Map.Entry.comparingByValue())
                // Transforms the Optional containing the entry pair into an Optional<String> containing just the winning word.
                // If the max element didn't exist, the Optional naturally stays empty.
                .map(Map.Entry::getKey);
    }
    /*
    Exactly How Optional.map() Handles the Data
    Think of the Optional returned by .max() as a secure cardboard box.
    If the box has something inside: Optional.map(Map.Entry::getKey) opens the box, takes out the winning Map.Entry pair
    (e.g., ["banana" = 12]), strips away the frequency count 12, takes just the key string "banana", puts "banana" back
    into a new box, and hands it to you as an Optional<String>.
    If the box is completely empty: (which happens if your alphabetical range had zero matching words),
    Optional.map() completely skips the conversion step. It doesn't break, it doesn't crash with a NullPointerException—
    it simply hands you back a clean, safe Optional.empty().
     */
}
