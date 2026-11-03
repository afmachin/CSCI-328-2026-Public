import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Iterator's misuse signature, in the baseline's own Roster.
 *
 * BaselineRoster is Roster.java from codebase/v0-baseline transcribed: a private
 * HashMap and a getter that returns it. CardRoster is the version on pattern card 04.
 * Same job, and only one of them can be corrupted by a caller.
 */
public class RosterCmeDemo {

    record Student(String id, String name) { }

    /** v0-baseline/src/registration/Roster.java, field and accessor unchanged. */
    static final class BaselineRoster {
        private Map<String, Student> students = new HashMap<>();
        public Map<String, Student> getStudents() { return students; }
    }

    /** reference/pattern-cards/04-behavioral-ii.md, the Iterator shape. */
    static final class CardRoster implements Iterable<Student> {
        private final Map<String, Student> byId = new LinkedHashMap<>();
        void add(Student s) { byId.put(s.id(), s); }
        public Iterator<Student> iterator() {
            return Collections.unmodifiableCollection(byId.values()).iterator();
        }
        public Stream<Student> stream() { return byId.values().stream(); }
    }

    public static void main(String[] args) {

        System.out.println("--- the baseline's Roster ---");
        BaselineRoster baseline = new BaselineRoster();
        baseline.getStudents().put("S1", new Student("S1", "Alvarez"));
        baseline.getStudents().put("S2", new Student("S2", "Brennan"));
        baseline.getStudents().put("S3", new Student("S3", "Chau"));
        System.out.println("getStudents() returns the live map, so a caller can drop a");
        System.out.println("student mid-traversal. Nothing in Roster's source forbids it:");
        int visited = 0;
        try {
            for (Student s : baseline.getStudents().values()) {
                visited++;
                if (visited == 1) {
                    // Drop some other student. Which one depends on HashMap's order,
                    // and that is part of the point -- the caller does not control it.
                    String victim = baseline.getStudents().keySet().stream()
                            .filter(k -> !k.equals(s.id())).findFirst().get();
                    baseline.getStudents().remove(victim);
                }
            }
            System.out.println("  traversal finished, visited " + visited + " of 3");
        } catch (java.util.ConcurrentModificationException e) {
            System.out.println("  visited " + visited + " of 3, then "
                    + e.getClass().getName());
        }
        System.out.println("The exception is the lucky case. It is thrown on a best-effort");
        System.out.println("basis, so the same code can also just skip a student silently.");

        System.out.println();
        System.out.println("--- card 04's Roster ---");
        CardRoster card = new CardRoster();
        card.add(new Student("S1", "Alvarez"));
        card.add(new Student("S2", "Brennan"));
        card.add(new Student("S3", "Chau"));
        System.out.print("for-each works: ");
        for (Student s : card) { System.out.print(s.name() + " "); }
        System.out.println();
        System.out.print("stream works:   ");
        card.stream().forEach(s -> System.out.print(s.id() + " "));
        System.out.println();
        System.out.print("remove() through the iterator: ");
        try {
            Iterator<Student> it = card.iterator();
            it.next();
            it.remove();
            System.out.println("removed -- which would be the bug");
        } catch (UnsupportedOperationException e) {
            System.out.println(e.getClass().getSimpleName());
        }
        System.out.println("No way in. The caller gets traversal and nothing else, which is");
        System.out.println("the whole of what the pattern is for.");
    }
}
