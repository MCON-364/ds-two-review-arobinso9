These three methods—headMap, tailMap, and subMap—are specialized tools available on sorted maps (like TreeMap and ConcurrentSkipListMap).
Because a sorted map maintains its keys in a strict alphabetical or numerical sequence, Java doesn't need to loop through the entire map to filter items.
Instead, it uses these methods to slice the map into a specific window instantaneously.

Here is exactly how each one works.

1. headMap(toKey)
   Think of headMap as grabbing the "head" (the beginning) of the map. 
It cuts a slice starting from the very first element up to a specific boundary point you define.
Behavior by default: The lower bound is inclusive (starts at the beginning), and the upper bound (toKey) is exclusive (stops just before it).

Example:
If you have a map of alphabetized names, map.headMap("Charlie") will give you a view of everyone whose name comes before Charlie:

Map Keys:   [ Alex, Bob, Charlie, David, Emily ]
└───┬───┘
headMap("Charlie") -> [ Alex, Bob ]

2. tailMap(fromKey)
   Think of tailMap as grabbing the "tail" (the end) of the map. 
It cuts a slice starting at your specific boundary point and goes all the way to the very last element of the map.
Behavior by default: The lower bound (fromKey) is inclusive (starts exactly on it), and the upper bound is inclusive (goes to the absolute end).

Example:
Using that same map of names, map.tailMap("Charlie") starts exactly at Charlie and grabs everything remaining:

Map Keys:   [ Alex, Bob, Charlie, David, Emily ]
└─────────┬─────────┘
tailMap("Charlie") -> [ Charlie, David, Emily ]

3. subMap(fromKey, toKey)
   subMap is a combination of both. It isolates a specific "sub-section" right out of the middle of your map by setting an explicit floor and ceiling.
Behavior by default: The starting point (fromKey) is inclusive, but the ending point (toKey) is exclusive.

Example:
map.subMap("Bob", "Emily") will slice the map starting exactly at Bob, up to but excluding Emily:

Map Keys:   [ Alex, Bob, Charlie, David, Emily ]
└───────┬───────┘
subMap("Bob", "Emily") -> [ Bob, Charlie, David ]


Changing the Inclusivity (The Modern Way)
By default, Java assumes you want the starting number/word included and the ending number/word excluded. 
But what if you want both sides to be inclusive?
Java allows you to pass extra boolean flags (true or false) right after your keys to force the exact behavior you need:

// Slices from "Bob" (inclusive) to "Emily" (inclusive)
log.subMap("Bob", true, "Emily", true);

// Slices from the beginning up to "Charlie" (inclusive)
log.headMap("Charlie", true);

// Slices from "Charlie" (exclusive) to the very end
log.tailMap("Charlie", false);

 4. firstKey() and lastKey()
These methods give you immediate access to the extreme boundaries of your dataset in O(log n) time
.firstKey(): Retrieves the absolute lowest key currently in the map (the first item alphabetically)
.lastKey(): Retrieves the absolute highest key currently in the map (the last item alphabetically).
Real-World Usage Example:Imagine you want to display the alphabetical scope of your dictionary report
(e.g., "This report covers words ranging from 'apple' to 'zebra'").JavaTreeMap<String, Long> freqMap = buildFrequencyMap();

if (!freqMap.isEmpty()) {
String alphabetBeginning = freqMap.firstKey(); // e.g., "apple"
String alphabetEnd       = freqMap.lastKey();  // e.g., "zebra"

    System.out.println("Range: " + alphabetBeginning + " to " + alphabetEnd);
}

TreeMap and ConcurrentSkipListMap both implement NavigableMap so hv access to these methods
You can copy and paste your entire WordFrequencyCounter logic, replace TreeMap::new with ConcurrentSkipListMap::new,
and every single method—including subMap(), firstKey(), and lastKey()—will work flawlessly without changing another
line of code.

MAPS VS SETS 
The Maps (TreeMap and ConcurrentSkipListMap) implement NavigableMap.
The Sets (TreeSet and ConcurrentSkipListSet) implement NavigableSet.

The Big Unified Picture
Because they all inherit from these Navigable parent interfaces, Java guarantees that they all share the exact same range-slicing
and boundary-finding superpowers.
If you are using a Map (NavigableMap)    If you are using a Set (NavigableSet)          What it does mathematically
.firstKey() / .lastKey()                  .first() / .last()                            Instantly grabs the absolute edges of your data.
.subMap(from, to)                         .subSet(from, to)                             Slices a specific middle chunk out of your data.
.headMap(to)                              .headSet(to)                                  Grabs everything from the beginning up to a fence-post.
.tailMap(from)                            .tailSet(from)                                 Grabs everything from a fence-post to the very end.