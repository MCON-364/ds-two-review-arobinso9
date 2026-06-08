package edu.touro.mcon364.finalreview.treesandthreads.exercises;

import edu.touro.mcon364.finalreview.treesandthreads.model.Employee;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * In-class Exercise 2 — Employee Roster (TreeMap + TreeSet + Streams)
 *
 * Scenario: a company has employees spread across several departments.
 * You need to organize them so that departments are listed alphabetically
 * and employees within each department are also sorted alphabetically by name.
 *
 * This exercise practises:
 * - TreeMap<String, TreeSet<Employee>> as a two-level sorted structure.
 * - Collectors.groupingBy with a TreeSet downstream collector.
 * - Stream flatMap to merge employees from all departments into one list.
 * - NavigableMap range queries (departments A-M vs N-Z).
 *
 * Before coding, think about:
 * - Why do we use TreeSet inside the map rather than ArrayList?
 * - What comparator drives the ordering inside each TreeSet?
 * - If two employees have the same name and department but different salaries,
 *   are they considered the same element inside the TreeSet?
 *
 * Requirements:
 * - The constructor receives the list of employees.
 * - buildRoster() returns a TreeMap<String, TreeSet<Employee>> grouping
 *   employees by department. Both the departments and the employees within
 *   each department must be in sorted order.
 * - getTopEarnerPerDepartment() returns a Map<String, Employee> with the
 *   highest-paid employee in each department.
 * - getAllEmployeesSorted() returns all employees across all departments in
 *   a single sorted list (alphabetical by name).
 * - getDepartmentsInRange(from, to) returns a NavigableMap slice containing
 *   only the departments whose names fall in [from, to] inclusive.
 *
 * Do not use explicit loops. Use streams and collectors.
 */
public class EmployeeRoster {

    private final List<Employee> employees;

    public EmployeeRoster(List<Employee> employees) {
        // TODO: validate non-null, store a defensive copy
        if (employees == null)
            throw new IllegalArgumentException("employees cannot be null");
        this.employees = List.copyOf(employees);
    }

    /**
     * Groups employees by department into a sorted two-level structure.
     *
     * @return sorted map: department name -> sorted set of employees
     */
    /*
    TreeMap is a Map: It stores Key-Value pairs (Map<K, V>).
    You look up a value (like a definition) using its unique key (like a word).
    TreeSet is a Set: It stores Individual unique elements (Set<E>).
    It is simply a collection of distinct items with no associated values.
     */
    public TreeMap<String, TreeSet<Employee>> buildRoster() {
        // TODO
        return employees.stream().collect(Collectors.groupingBy(
                employee -> employee.department(), //dept is the key
                TreeMap::new, // we want it in a sorted TreeMap instead of default HashSet
                Collectors.toCollection(TreeSet::new) // the value is a TreeSet- a sorted set of employees- value: treeset of Employees
        ));
    }

    /**
     * Returns the highest-paid employee in each department.
     *
     * @return map of department name -> top earner
     */
    public Map<String, Employee> getTopEarnerPerDepartment() {
        // TODO
        // we get the key/value pairs of (dept: treeMap of employees) and stream them
        return buildRoster().entrySet().stream()
                // We build a brand-new map from this stream.- this is 1:1. we use toMap()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,//key - the dept - Keeps the department name as the key in the new map.
                        // For each department, we extract the TreeSet value, turn that internal set into a stream of employees
                        entry -> entry.getValue().stream()
                                // we then use .max(Comparator.comparingDouble(Employee::salary)) to evaluate their salaries and find the highest-paid individual.
                                // extract their salaries and get max per dept
                                .max(Comparator.comparingDouble(Employee::salary))
                                // we use .orElseThrow() as a tool to tell the compiler: "Open this Optional box and give me the raw Employee inside.
                                // I promise you it isn't empty, but if it is, crash the program." Because of the rules of groupingBy in Step 1, it will never be empty, making it 100% safe!
                                // Inside buildRoster(), Collectors.groupingBy() runs. By definition, groupingBy will only create a department
                                // bucket if there is at least one employee belonging to it. It will never create an empty bucket. There4 there will always be a max
                                .orElseThrow()
                ));
    }

    /**
     * Returns every employee across all departments in a single alphabetical list.
     *
     *
     * @return globally sorted employee list
     */
    public List<Employee> getAllEmployeesSorted() {
        // TODO
        // we get the values: treeSets of employees and stream all the treeSets
        return buildRoster().values().stream()
                //unbox the TreeSets, so we are working with Employee objects now
                .flatMap(Collection::stream)
                // just bc they were sorted by dept - does not at all mean that when we flatMap the employees they will be sorted when combined
                // there4 we need to sort all employees.
                // to make sure that all employees from all depts are in alphabetical order we need to sort them by name
                .sorted(Comparator.comparing(Employee::name))
                // convert to list.
                .toList();
    }

    /**
     * Returns a view of the roster containing only departments in [from, to].
     *
     *
     * @param from lower bound department name (inclusive)
     * @param to   upper bound department name (inclusive)
     * @return navigable sub-map
     */
    public NavigableMap<String, TreeSet<Employee>> getDepartmentsInRange(String from, String to) {
        // TODO
        if (from.compareTo(to) > 0) {
            return new TreeMap<>();
        }
        // .subMap(from, true, to, true):  we pass boolean flags here.
        // Setting both to true ensures that both the from word and the to word are inclusive in our slice.
        // default is: from= inclusive. to= exclusive
        return buildRoster().subMap(from, true, to, true);
    }
}

