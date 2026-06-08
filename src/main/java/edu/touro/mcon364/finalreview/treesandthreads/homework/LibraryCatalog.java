package edu.touro.mcon364.finalreview.treesandthreads.homework;

import edu.touro.mcon364.finalreview.treesandthreads.model.Book;
import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Homework 1 - Library Catalog (TreeMap + TreeSet + Streams)
 *
 * Scenario: a library stores books. Each book has a title, author, and
 * publication year. The catalog must answer several questions in sorted order.
 *
 * Before coding, think about:
 * - Which structure gives us books sorted by title automatically?
 * - Should the author-to-books index use a List or a Set inside the map?
 *   What happens if the same book appears twice?
 * - What does NavigableMap.headMap give us, and when would we use it?
 *
 * Requirements:
 * - The constructor receives the list of books to index.
 * - buildTitleIndex() returns a TreeMap keyed by title for O(log n) exact lookups.
 * - buildAuthorIndex() returns a TreeMap grouping books by author; books for each
 *   author are sorted by title.
 * - getBooksPublishedBefore(year) returns all books published strictly before
 *   the given year, sorted by title.
 * - getAuthorsWithMoreThan(n) returns a sorted list of author names who have
 *   more than n books in the catalog.
 * - findByTitlePrefix(prefix) returns all books whose title starts with the
 *   given string, alphabetically. Use NavigableMap range operations.
 *
 * Do not use explicit loops. Use streams and collectors.
 */
public class LibraryCatalog {

    private final List<Book> books;

    public LibraryCatalog(List<Book> books) {
        // TODO: validate non-null, store a defensive copy
        if (books == null) throw new NullPointerException();
        this.books = List.copyOf(books);
    }

    /**
     * Returns a TreeMap keyed by book title for O(log n) exact lookups.
     * If two books share a title, keep only one (your choice which).
     *
     */
    // since its 1:1 - we need a single Book as a value per title, we use Collectors.toMap not Collectors.groupingBy()
    public TreeMap<String, Book> buildTitleIndex() {
        // TODO
        return books.stream()
                .collect(Collectors.toMap(
                        Book::title, //title is the key
                        Function.identity(), //the value is the Book obj
                        // Provide a merge function to satisfy the requirement: "If two books share a title, keep only one"
                        (existing, replacement) -> existing, // Keeps the first book if duplicate titles exist
                        TreeMap::new // groupingBy by default creates a HashMap, so here we are creating a TreeMap instead
                ));
    }

    /**
     * Returns a TreeMap grouping books by author; each author maps to a
     * TreeSet of their books sorted by title.
     * 1:M
     */
    public TreeMap<String, TreeSet<Book>> buildAuthorIndex() {
        // TODO
        return books.stream()
                .collect(Collectors.groupingBy(
                        Book::author, // author is the key
                        TreeMap::new, // groupingBy by default creates a HashMap, so here we are creating a TreeMap instead
                        Collectors.toCollection(TreeSet::new) // the value is a TreeSet- a sorted set of books by title - see the Book classes compareTo method
                        // 1) if we didn't want TreeSet, then wld do Collectors.toList() instead of Collectors.toCollection(TreeSet::new).
                ));
    }

    /**
     * Returns all books published strictly before the given year, sorted by title.
     * buildTitleIndex() -> each title points to exactly one Book obj
     */
    public List<Book> getBooksPublishedBefore(int year) {
        // TODO
        return buildTitleIndex().values().stream() // we stream the book objects
                .filter(book -> book.year() < year)
                // we wld do .sorted() if it wasn't already sorted
                .toList(); // Already sorted by title because values come from buildTitleIndex()
    }

    /**
     * Returns a sorted list of author names who have more than n books in this catalog.
     * buildAuthorIndex() -> each author points to a TreeSet of Book objects
     * since we need access to both the key and value we use .entrySet()
     */
    public List<String> getAuthorsWithMoreThan(int n) {
        // TODO
        return buildAuthorIndex().entrySet().stream()
                .filter(entry -> entry.getValue().size() > n) // Inspects the size of the TreeSet - to see how many books it contains
                .map(Map.Entry::getKey) // Extract the author's name string
                .toList(); // Automatically sorted alphabetically because source map is a TreeMap
    }

    /**
     * Returns all books whose title starts with the given prefix, alphabetically.
     *
     */
    /*
    In the word frequency counter, the method parameters gave you a single char (like 'b').
    Because it was a single character, we could just look at the next letter in the alphabet ('b' + 1 = 'c').
    We then sliced the map from "b" to "c".
    Because TreeMap.subMap(start, end) is exclusive on the end bound, it grabs everything starting with "b",
    up to but excluding anything that starts with "c". This naturally catches words like "banana", "blueberry", and "box".

    In the library catalog, the method parameter is a full-length String prefix (like "java").
    If you try to just add 1 to the entire string or treat it like a single character, the math breaks.
    We can't do "java" + 1. We have to find a way to tell the TreeMap where the "java" section ends in a dictionary.
    To do that, we isolate the very last character of that string ('a') and increment just that letter to 'b'.
    Prefix: "java"
    Start:  "java"
    End:    "javb"  (Change the last letter 'a' to 'b')
    Because the end bound is exclusive, subMap("java", "javb") grabs every title that starts with "java",
    up to but excluding anything that starts with "javb".

    Alphabetical Book Titles:
   "javascript for beginners"   <-- Included (Starts with "java")
   "java threads"               <-- Included (Starts with "java")
   "java: a beginner's guide"   <-- Included (Starts with "java")
============================
   "javb manual"                <-- EXCLUDED! (This is our 'end' boundary line)
   "jellyfish facts"            <-- Excluded

   Summary
    In the word counter, you sliced from "b" to "c".
    In the library catalog, you sliced from "java" to "javb".
     */
    public List<Book> findByTitlePrefix(String prefix) {
        // TODO
        if (prefix == null || prefix.isEmpty()) {
            return buildTitleIndex().values().stream().toList();
        }

        // Calculate string range boundaries (e.g., "java" -> "javb")
        String start = prefix;
        // we get the lastChar of the prefix word - For "java", this is 'a'
        char lastChar = prefix.charAt(prefix.length() - 1);
        // Step A: Chop off the last letter
        // prefix.substring(0, prefix.length() - 1)
        // This grabs everything in the string except for the very last character. It takes a slice from index 0 up to the second-to-last index.
        // Example: If the prefix is "java", this turns into "jav".
        // Step B: Increment the last letter by 1
        // (char) (lastChar + 1)
        // Computers see characters as numeric ASCII/Unicode values. The character 'a' is represented by the number 97.
        // Adding 1 changes it to 98.The (char) cast converts that number 98 back into a letter, which is 'b'.
        // Step C: Glue them back together
        // Finally, the code uses the + operator to merge the pieces back together:
        // "jav" + 'b' = "javb"
        // A TreeMap keeps its keys sorted like a real-world dictionary.
        // When you call .subMap(start, end), the end bound is exclusive (meaning it means "up to, but not including this exact word").
        // If we want to find every book that starts with "java", we need to tell Java exactly where the "java" section ends in the dictionary.
        //By calculating "javb", we create a perfect boundary wall
        String end = prefix.substring(0, prefix.length() - 1) + (char) (lastChar + 1);

        // Slice title index map instantaneously via subMap
        // Why .values(): The method doesn't want a list of title strings; it wants the actual full Book objects,
        // which are stored as the values inside that map.
        return new ArrayList<>(buildTitleIndex().subMap(start, end).values());

    }
}

/*
Implementation 1: Finding Authors Starting with a Letter
If you want to find all authors whose names start with a certain character,
you grab the keySet() of your author index (which is a sorted set of strings) and slice it using subSet:

public List<String> getAuthorsStartingWith(char prefix) {
    String start = String.valueOf(prefix);
    String end   = String.valueOf((char) (prefix + 1));

    // Route A: Slice the TreeMap first, then extract the standard keySet
    return new ArrayList<>(buildAuthorIndex().subMap(start, end).keySet());
}

Implementation 2: Finding a Specific Author's Books Starting with a Letter
What if you have an author's TreeSet<Book> from buildAuthorIndex().get(authorName),
and you want to pull only their books that start with a specific letter?
Because a TreeSet sorts things based on its Comparator (which in your Book class sorts by title),
you cannot just pass raw strings into subSet. You must pass dummy Book objects containing the boundary titles to match how the tree calculates positions!

public List<Book> getBooksByAuthorStartingWith(String authorName, char prefix) {
    TreeSet<Book> authorBooks = buildAuthorIndex().get(authorName);
    if (authorBooks == null) return List.of();

    // Create a dummy start book (e.g., Title: "b", Year/Author don't matter for sorting)
    Book startBook = new Book(String.valueOf(prefix), authorName, 0);

    // Create a dummy end book (e.g., Title: "c")
    Book endBook   = new Book(String.valueOf((char) (prefix + 1)), authorName, 0);

    // subSet works on the TreeSet of books using the dummy boundaries!
    return new ArrayList<>(authorBooks.subSet(startBook, endBook));
}
 */

/*
Imagine the requirements change, or a new method is added called getSortedBooksByAuthor(String author).
Because your index now stores books in a regular ArrayList (the order they happened to arrive in), you can no longer just pull the collection and return it. You are forced to introduce an explicit .sorted() step into a stream pipeline to sort them by title manually:

public List<Book> getSortedBooksByAuthor(String author) {
    List<Book> unsortedBooks = buildAuthorIndex().get(author);
    if (unsortedBooks == null) return List.of();

    // We must manually sort now because the List didn't do it for us!
    return unsortedBooks.stream()
            .sorted(Comparator.comparing(Book::title))
            .toList();
}
 */