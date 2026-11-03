import java.util.Iterator;
import java.util.List;

/**
 * External and internal iteration, and the three things you give up when you hand
 * the loop to the collection.
 *
 * Nothing here is a trick. Every one of the three is a thing the caller could do
 * trivially while it owned the loop, and cannot do once it does not.
 */
public class IterationControlDemo {

    record Student(String id, String name, int credits) { }

    static final List<Student> ROSTER = List.of(
            new Student("S1", "Alvarez", 12),
            new Student("S2", "Brennan", 15),
            new Student("S3", "Chau",    18),
            new Student("S4", "Dube",    21),
            new Student("S5", "Essien",  9));

    static final List<String> ADVISORS = List.of("Okafor", "Petrov", "Quinn", "Reyes", "Sato");

    public static void main(String[] args) {

        System.out.println("--- 1. stopping early ---");
        System.out.print("external, stop at the first student over 16 credits: visited ");
        for (Student s : ROSTER) {
            System.out.print(s.id() + " ");
            if (s.credits() > 16) { System.out.println("-> stopping at " + s.name()); break; }
        }
        System.out.print("internal, forEach with the same test:              visited ");
        ROSTER.forEach(s -> System.out.print(s.id() + " "));
        System.out.println("-> all five, no way out");
        System.out.println("A stream can short-circuit, but only with an operation that is");
        System.out.println("built to: findFirst, anyMatch, takeWhile. forEach is not one.");
        System.out.print("stream, findFirst:                                 ");
        System.out.println(ROSTER.stream().filter(s -> s.credits() > 16).findFirst().get().name());

        System.out.println();
        System.out.println("--- 2. stepping two sequences together ---");
        System.out.println("external, one advisor per student, in order:");
        Iterator<Student> students = ROSTER.iterator();
        Iterator<String> advisors = ADVISORS.iterator();
        while (students.hasNext() && advisors.hasNext()) {
            System.out.println("  " + students.next().name() + " -> " + advisors.next());
        }
        System.out.println("Each loop body advances both. Neither collection knows the other");
        System.out.println("exists, and no index arithmetic appears anywhere.");
        System.out.println("internal: forEach gives each lambda one element and no position,");
        System.out.println("so pairing needs an external counter, which is the loop variable");
        System.out.println("you just gave away.");

        System.out.println();
        System.out.println("--- 3. resuming ---");
        Iterator<Student> it = ROSTER.iterator();
        System.out.print("read two: ");
        System.out.print(it.next().name() + " " + it.next().name());
        System.out.println("   (iterator kept, work done elsewhere)");
        System.out.print("resume:   ");
        while (it.hasNext()) { System.out.print(it.next().name() + " "); }
        System.out.println();
        System.out.println("The iterator IS the position, so it can be put down and picked up.");
        System.out.println("forEach has no position to hand back.");

        System.out.println();
        System.out.println("Control you keep is flexible for you.");
        System.out.println("Control you hand over is flexible for the implementation.");
    }
}
