import java.util.List;

/**
 * The three things a static factory method can do and a constructor cannot.
 * 
 *
 *   cd demos/static-factory
 *   javac StaticFactoryDemo.java && java StaticFactoryDemo
 *
 * The recorded output is in output.txt beside this file.
 *
 * Nothing here needs the semester codebase. Part four is the one worth running twice:
 * the two numbers it prints are a language guarantee, not an accident of this JVM.
 */
public class StaticFactoryDemo {

    public static void main(String[] args) {
        partOne();
        System.out.println();
        partTwo();
        System.out.println();
        partThree();
        System.out.println();
        partFour();
    }

    /* ------------------------------------------------------------------ */

    /** A name, and two of them taking the same parameter types. */
    record Credits(int value) {
        static Credits earned(int n)    { return new Credits(n); }
        static Credits attempted(int n) { return new Credits(n); }
    }

    static void partOne() {
        System.out.println("== part one: names ==");
        System.out.println("Credits.earned(12)    -> " + Credits.earned(12));
        System.out.println("Credits.attempted(12) -> " + Credits.attempted(12));
        System.out.println();
        System.out.println("As constructors these two do not compile. Both take one int, so");
        System.out.println("the compiler has no way to tell them apart:");
        System.out.println("    error: constructor Credits(int) is already defined in class Credits");
    }

    /* ------------------------------------------------------------------ */

    /**
     * A cached instance. The constructor is private, so `closed()` is the only way to
     * get a closed window and it can hand back the same one every time.
     */
    static final class EnrollmentWindow {

        private static final EnrollmentWindow CLOSED = new EnrollmentWindow(0, 0);

        private final long opens;
        private final long closes;

        private EnrollmentWindow(long opens, long closes) {
            this.opens = opens;
            this.closes = closes;
        }

        static EnrollmentWindow closed()    { return CLOSED; }
        static EnrollmentWindow immediate() { return new EnrollmentWindow(0, 3600); }

        @Override public String toString() { return "EnrollmentWindow[" + opens + ".." + closes + "]"; }
    }

    static void partTwo() {
        System.out.println("== part two: an instance that already exists ==");
        System.out.println("closed()    -> " + EnrollmentWindow.closed());
        System.out.println("immediate() -> " + EnrollmentWindow.immediate());
        System.out.println();
        System.out.println("closed() == closed()       -> "
                + (EnrollmentWindow.closed() == EnrollmentWindow.closed())
                + "    one shared instance");
        System.out.println("immediate() == immediate() -> "
                + (EnrollmentWindow.immediate() == EnrollmentWindow.immediate())
                + "   a new one each time");
        System.out.println();
        System.out.println("`new` is defined to produce a fresh object every time, so a");
        System.out.println("constructor cannot do the first line. Only the factory can choose.");
    }

    /* ------------------------------------------------------------------ */

    interface EligibilityRule { String describe(); }

    /** Hidden on purpose. In real code this would be package-private in its own file;
     *  here it is private to the demo, and either way a caller cannot name it. */
    private record CreditLimitRule(int max) implements EligibilityRule {
        public String describe() { return "at most " + max + " credits"; }
    }

    static EligibilityRule creditLimit(int max) { return new CreditLimitRule(max); }

    static void partThree() {
        System.out.println("== part three: a subtype you cannot name ==");
        EligibilityRule rule = creditLimit(18);
        System.out.println("creditLimit(18).describe() -> " + rule.describe());
        System.out.println("declared type              -> EligibilityRule");
        System.out.println("actual class               -> " + rule.getClass().getSimpleName());
        System.out.println();
        System.out.println("The JDK does this to you constantly:");
        System.out.println("List.of(1, 2)              -> " + List.of(1, 2));
        System.out.println("its class                  -> "
                + List.of(1, 2).getClass().getSimpleName()
                + "   (a JDK-internal type for one- and two-element lists)");
        System.out.println();
        System.out.println("You cannot write that name, cannot extend it, and it can be");
        System.out.println("replaced in the next release without breaking your code.");
    }

    /* ------------------------------------------------------------------ */

    static void partFour() {
        System.out.println("== part four: the one in the JDK you have already used ==");
        System.out.println("Integer.valueOf(127) == Integer.valueOf(127) -> "
                + (Integer.valueOf(127) == Integer.valueOf(127)));
        System.out.println("Integer.valueOf(128) == Integer.valueOf(128) -> "
                + (Integer.valueOf(128) == Integer.valueOf(128)));
        System.out.println();
        System.out.println("valueOf caches -128 to 127 and allocates above it. A constructor");
        System.out.println("could not return true on the first line at all, which is why");
        System.out.println("new Integer(int) is deprecated for removal and valueOf is not.");
        System.out.println();
        System.out.println("Caching is only safe because Integer is immutable. Share a mutable");
        System.out.println("object between callers and each one is editing everyone else's.");
    }
}
