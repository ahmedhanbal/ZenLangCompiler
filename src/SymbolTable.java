import java.util.*;

/**
 * SymbolTable.java
 * Tracks every identifier encountered during scanning.
 *
 * For each unique name the table records:
 *   - the name itself
 *   - a type annotation (filled in by later compiler phases)
 *   - the line and column of its first appearance
 *   - how many times it has been seen
 */
public class SymbolTable {

    // ── Inner record ─────────────────────────────────────────────────────────

    /**
     * Holds metadata for a single identifier.
     */
    private static class Entry {
        final String name;
        String       declaredType;   // set during semantic analysis
        final int    firstLine;
        final int    firstCol;
        int          occurrences;

        Entry(String name, int line, int col) {
            this.name         = name;
            this.declaredType = "unknown";
            this.firstLine    = line;
            this.firstCol     = col;
            this.occurrences  = 1;
        }

        void bump() { occurrences++; }

        @Override
        public String toString() {
            return String.format("%-22s | %-12s | Line: %-4d Col: %-4d | Count: %d",
                                 name, declaredType, firstLine, firstCol, occurrences);
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Insertion-ordered map so the table prints in discovery order. */
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Records an identifier occurrence.
     * If the name is new it is added; otherwise its count is incremented.
     */
    public void record(String name, int line, int col) {
        if (entries.containsKey(name)) {
            entries.get(name).bump();
        } else {
            entries.put(name, new Entry(name, line, col));
        }
    }

    /** Returns true if the name has been seen at least once. */
    public boolean has(String name) {
        return entries.containsKey(name);
    }

    /** Returns how many times the name has appeared (0 if unknown). */
    public int countOf(String name) {
        Entry e = entries.get(name);
        return (e != null) ? e.occurrences : 0;
    }

    /** Returns the number of distinct identifiers recorded. */
    public int uniqueCount() {
        return entries.size();
    }

    /**
     * Returns all entries sorted by occurrence count, descending.
     * Useful for statistics output.
     */
    public List<Map.Entry<String, Entry>> byFrequency() {
        List<Map.Entry<String, Entry>> list = new ArrayList<>(entries.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue().occurrences,
                                            a.getValue().occurrences));
        return list;
    }

    /** Prints the table to standard output in a formatted layout. */
    public void display() {
        final int W = 88;
        System.out.println("\n" + "=".repeat(W));
        System.out.println("IDENTIFIER TABLE");
        System.out.println("=".repeat(W));

        if (entries.isEmpty()) {
            System.out.println("  (no identifiers found)");
        } else {
            System.out.printf("%-22s | %-12s | %-18s | %s%n",
                              "Name", "Type", "First Occurrence", "Count");
            System.out.println("-".repeat(W));
            for (Entry e : entries.values()) {
                System.out.println("  " + e);
            }
            System.out.println("-".repeat(W));
            System.out.println("  Unique identifiers: " + entries.size());
        }
        System.out.println("=".repeat(W) + "\n");
    }
}
