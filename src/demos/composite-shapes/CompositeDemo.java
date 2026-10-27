import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Composite, written twice: the 1994 shape and the sealed-interface shape, from note 07.
 *
 *   cd demos/composite-shapes
 *   javac CompositeDemo.java && java CompositeDemo
 *
 * The recorded output is in output.txt beside this file.
 *
 * Both model the same prerequisite rule: CS201, and either MATH150 or MATH160.
 * Part one ends in the exception the classical shape forces on its leaves.
 *
 * The third exhibit does not compile on purpose and so is not in this file. It is
 * demos/composite-missing-case/, which holds CompositeMissingCase.java and the
 * compiler's answer in javac-output.txt.
 */
public class CompositeDemo {

    record Transcript(Set<String> passed) {
        boolean hasPassed(String code) { return passed.contains(code); }
    }

    public static void main(String[] args) {
        Transcript dana = new Transcript(Set.of("CS201", "MATH160"));
        partOne(dana);
        System.out.println();
        partTwo(dana);
    }

    /* ================================================================
       1994: one interface, every operation on it, leaves that refuse
       ================================================================ */

    interface Requirement {
        boolean satisfiedBy(Transcript t);
        void add(Requirement child);
        void remove(Requirement child);
        List<Requirement> children();
    }

    static final class CourseRequirement implements Requirement {
        private final String code;
        CourseRequirement(String code) { this.code = code; }

        public boolean satisfiedBy(Transcript t) { return t.hasPassed(code); }

        // The three below exist only because the interface demands them.
        public void add(Requirement child)    { throw new UnsupportedOperationException("a course has no children"); }
        public void remove(Requirement child) { throw new UnsupportedOperationException("a course has no children"); }
        public List<Requirement> children()   { return List.of(); }

        @Override public String toString() { return code; }
    }

    static final class AllOfNode implements Requirement {
        private final List<Requirement> parts = new ArrayList<>();
        public boolean satisfiedBy(Transcript t) {
            for (Requirement p : parts) if (!p.satisfiedBy(t)) return false;
            return true;
        }
        public void add(Requirement child)    { parts.add(child); }
        public void remove(Requirement child) { parts.remove(child); }
        public List<Requirement> children()   { return parts; }
        @Override public String toString() { return "AllOf" + parts; }
    }

    static final class AnyOfNode implements Requirement {
        private final List<Requirement> parts = new ArrayList<>();
        public boolean satisfiedBy(Transcript t) {
            for (Requirement p : parts) if (p.satisfiedBy(t)) return true;
            return false;
        }
        public void add(Requirement child)    { parts.add(child); }
        public void remove(Requirement child) { parts.remove(child); }
        public List<Requirement> children()   { return parts; }
        @Override public String toString() { return "AnyOf" + parts; }
    }

    static void partOne(Transcript dana) {
        System.out.println("== part one: the 1994 shape ==");

        AnyOfNode eitherMath = new AnyOfNode();
        eitherMath.add(new CourseRequirement("MATH150"));
        eitherMath.add(new CourseRequirement("MATH160"));

        AllOfNode rule = new AllOfNode();
        rule.add(new CourseRequirement("CS201"));
        rule.add(eitherMath);

        System.out.println("rule              -> " + rule);
        System.out.println("satisfiedBy(dana) -> " + rule.satisfiedBy(dana));
        System.out.println();
        System.out.println("It works, and the uniformity is real: nothing above had to ask whether");
        System.out.println("it was holding a course or a group. That is what the pattern buys.");
        System.out.println();
        System.out.println("Now walk the tree generically and try to edit a node:");

        Requirement firstChild = rule.children().get(0);
        System.out.println("   first child            -> " + firstChild);
        try {
            firstChild.add(new CourseRequirement("CS101"));
        } catch (UnsupportedOperationException e) {
            System.out.println("   firstChild.add(...)    -> UnsupportedOperationException: " + e.getMessage());
        }
        System.out.println();
        System.out.println("Nothing in the type system warned the caller. `add` is on Requirement,");
        System.out.println("so every Requirement claims to accept children and one third of them");
        System.out.println("are lying. That is the cost of putting add/remove on the shared type.");
    }

    /* ================================================================
       Modern: a sealed interface, records, no operations that do not apply
       ================================================================ */

    sealed interface Req {
        record Course(String code) implements Req {}
        record AllOf(List<Req> parts) implements Req {}
        record AnyOf(List<Req> parts) implements Req {}
    }

    static boolean satisfiedBy(Req req, Transcript t) {
        return switch (req) {
            case Req.Course c -> t.hasPassed(c.code());
            case Req.AllOf a  -> a.parts().stream().allMatch(p -> satisfiedBy(p, t));
            case Req.AnyOf a  -> a.parts().stream().anyMatch(p -> satisfiedBy(p, t));
        };
    }

    static void partTwo(Transcript dana) {
        System.out.println("== part two: the sealed shape ==");

        Req rule = new Req.AllOf(List.of(
                new Req.Course("CS201"),
                new Req.AnyOf(List.of(new Req.Course("MATH150"), new Req.Course("MATH160")))));

        System.out.println("rule              -> " + rule);
        System.out.println("satisfiedBy(dana) -> " + satisfiedBy(rule, dana));
        System.out.println();
        System.out.println("Same answer, same uniformity -- satisfiedBy never asks what kind of node");
        System.out.println("it has, the switch does that once. Three differences from part one:");
        System.out.println();
        System.out.println("  1. Course has no add or remove, because it has no children. There is");
        System.out.println("     nothing to throw from and nothing to document as unsupported.");
        System.out.println("  2. The switch has no default branch and compiles anyway, because the");
        System.out.println("     compiler knows the three cases are all of them. Add a fourth node");
        System.out.println("     type and every switch like this one stops compiling -- see the");
        System.out.println("     composite-missing-case demo next door.");
        System.out.println("  3. Nothing mutates. The tree is a value, so it can be shared, compared");
        System.out.println("     with equals, and printed -- the toString above is free.");
        System.out.println();
        System.out.println("What it gives up: `sealed` means no code outside this file can add a");
        System.out.println("fourth kind of Req. For a catalog rule you own, that is the point.");
        System.out.println("For a library someone else extends, it is the wrong trade.");
    }
}
