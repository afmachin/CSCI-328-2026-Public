import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The other two halves of the Composite section.
 *
 *   cd reference/demos/composite-trade
 *   javac CompositeTradeDemo.java && java CompositeTradeDemo
 *
 * The recorded output is in output.txt beside this file.
 *
 * Part one is the SAFE design and the instanceof branches it pushes onto clients.
 * Part two is what "add a node" costs once the tree is a value and there is no add.
 * CompositeDemo.java beside this file has the transparent design and the sealed one.
 */
public class CompositeTradeDemo {
    record Transcript(Set<String> passed) { boolean hasPassed(String c){return passed.contains(c);} }

    /* ---- the SAFE design: add/remove only where they work ---- */
    interface Requirement { boolean satisfiedBy(Transcript t); }

    record CourseReq(String code) implements Requirement {
        public boolean satisfiedBy(Transcript t) { return t.hasPassed(code); }
    }
    static final class GroupReq implements Requirement {
        private final List<Requirement> parts = new ArrayList<>();
        public boolean satisfiedBy(Transcript t) {
            for (Requirement p : parts) if (!p.satisfiedBy(t)) return false; return true; }
        void add(Requirement r) { parts.add(r); }          // only here
        void remove(Requirement r) { parts.remove(r); }    // only here
        List<Requirement> parts() { return parts; }
        public String toString() { return "Group" + parts; }
    }

    /** The client that wanted to walk the tree and add a node under each group. */
    static int addUnderEveryGroup(Requirement node, Requirement toAdd) {
        int added = 0;
        if (node instanceof GroupReq g) {              // branch 1: is it a group?
            g.add(toAdd);
            added++;
            for (Requirement child : List.copyOf(g.parts())) {
                if (child instanceof GroupReq) {       // branch 2: recurse only into groups
                    added += addUnderEveryGroup(child, toAdd);
                }
            }
        }
        return added;
    }

    /* ---- the SEALED design, and what "add a node" costs ---- */
    sealed interface Req {
        record Course(String code) implements Req {}
        record AllOf(List<Req> parts) implements Req {}
    }

    /** No add exists. Adding means rebuilding every node from the root down. */
    static Req withChildAddedTo(Req node, Req target, Req child) {
        return switch (node) {
            case Req.Course c -> c;
            case Req.AllOf a -> {
                List<Req> rebuilt = new ArrayList<>();
                for (Req p : a.parts()) rebuilt.add(withChildAddedTo(p, target, child));
                if (a.equals(target)) rebuilt.add(child);
                yield new Req.AllOf(rebuilt);
            }
        };
    }

    public static void main(String[] x) {
        Transcript dana = new Transcript(Set.of("CS201"));

        GroupReq inner = new GroupReq(); inner.add(new CourseReq("MATH150"));
        GroupReq root = new GroupReq(); root.add(new CourseReq("CS201")); root.add(inner);

        System.out.println("-- safe design, the same client loop as the transparent one --");
        System.out.println("before: " + root);
        int n = addUnderEveryGroup(root, new CourseReq("CS101"));
        System.out.println("after : " + root);
        System.out.println("groups found: " + n + "; nothing threw, and the client needed two");
        System.out.println("instanceof branches it did not need in the transparent version.");

        System.out.println();
        System.out.println("-- sealed design: there is no add, so adding rebuilds the path --");
        Req target = new Req.AllOf(List.of(new Req.Course("MATH150")));
        Req tree = new Req.AllOf(List.of(new Req.Course("CS201"), target));
        System.out.println("before: " + tree);
        Req grown = withChildAddedTo(tree, target, new Req.Course("CS101"));
        System.out.println("after : " + grown);
        System.out.println("original unchanged: " + tree);
        System.out.println("satisfiedBy still reads in one switch; editing took a whole function.");
    }
}
