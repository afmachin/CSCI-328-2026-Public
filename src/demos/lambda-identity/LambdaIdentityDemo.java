import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Why Observer does not collapse.
 *
 *   cd demos/lambda-identity
 *   javac LambdaIdentityDemo.java && java LambdaIdentityDemo
 *
 * The recorded output is in output.txt beside this file.
 *
 * Part two is the one to run before you believe the note. The rule it prints is not a
 * detail of this JVM: a lambda that captures nothing is instantiated once and reused,
 * and a lambda that captures something is a new object every time it is evaluated.
 * The Observer bug in part four is that rule, arriving as a memory leak.
 */
public class LambdaIdentityDemo {

    interface WaitlistPolicy {
        int rank(String studentId);
    }

    public static void main(String[] args) {
        System.out.println("running on Java " + System.getProperty("java.version"));
        System.out.println();
        partOne();
        System.out.println();
        partTwo();
        System.out.println();
        partThree();
        System.out.println();
        partFour();
        System.out.println();
        new LambdaIdentityDemo().partFive();
    }

    /* ------------------------------------------------------------------ */

    /** What you actually see when a lambda turns up in a log or a debugger. */
    static void partOne() {
        System.out.println("== part one: what a lambda is called ==");

        WaitlistPolicy lambda = id -> id.length();
        WaitlistPolicy methodRef = String::length;
        WaitlistPolicy anonymous = new WaitlistPolicy() {
            public int rank(String id) { return id.length(); }
        };
        WaitlistPolicy named = new SeniorityPolicy();

        System.out.println("lambda           -> " + lambda);
        System.out.println("method reference -> " + methodRef);
        System.out.println("anonymous class  -> " + anonymous);
        System.out.println("named class      -> " + named);
        System.out.println();
        System.out.println("You get the enclosing class and an opaque hash. You do not get which");
        System.out.println("lambda in that class, and there is nothing to search the codebase for.");
        System.out.println("The anonymous class at least has a name you can locate; the named one");
        System.out.println("is the only one you could say out loud to a colleague at 2 a.m.");
    }

    static final class SeniorityPolicy implements WaitlistPolicy {
        public int rank(String id) { return id.length(); }
        @Override public String toString() { return "SeniorityPolicy"; }
    }

    /* ------------------------------------------------------------------ */

    /** The rule the note's Observer bug rests on. */
    static void partTwo() {
        System.out.println("== part two: when is the same lambda the same object? ==");

        System.out.println("capturing nothing, evaluated twice     -> "
                + (capturesNothing() == capturesNothing()) + "   one cached instance");

        Object auditLog = new Object();
        System.out.println("capturing auditLog, evaluated twice    -> "
                + (captures(auditLog) == captures(auditLog)) + "  a new object each time");

        System.out.println();
        System.out.println("That is the rule. It matters because every realistic listener captures");
        System.out.println("something -- the log, the waitlist, the service it calls -- so every");
        System.out.println("realistic listener is a fresh object on every evaluation.");
    }

    static WaitlistPolicy capturesNothing() { return id -> id.length(); }
    static WaitlistPolicy captures(Object captured) {
        return id -> id.length() + captured.hashCode() * 0;
    }

    /* ------------------------------------------------------------------ */

    /** Two identical bodies are never the same object and never equal. */
    static void partThree() {
        System.out.println("== part three: two identical bodies ==");
        WaitlistPolicy a = id -> id.length();
        WaitlistPolicy b = id -> id.length();
        System.out.println("a == b      -> " + (a == b));
        System.out.println("a.equals(b) -> " + a.equals(b)
                + "   lambdas do not override equals, so this is reference equality");
    }

    /* ------------------------------------------------------------------ */

    /** The leak, and the two fixes. */
    static void partFour() {
        System.out.println("== part four: subscribe, then try to unsubscribe ==");

        Object auditLog = new Object();
        List<Consumer<String>> listeners = new ArrayList<>();

        listeners.add(event -> record(auditLog, event));
        System.out.println("after subscribe                     -> " + listeners.size() + " listener");

        boolean removed = listeners.remove((Consumer<String>) (event -> record(auditLog, event)));
        System.out.println("remove(an identical lambda body)    -> returned " + removed
                + ", still " + listeners.size() + " listener");
        System.out.println();
        System.out.println("Nothing threw. Nothing was logged. The listener is still subscribed and");
        System.out.println("everything it captured is still reachable. That is the leak.");

        System.out.println();
        System.out.println("Fix one -- keep the reference:");
        Consumer<String> held = event -> record(auditLog, event);
        listeners.add(held);
        System.out.println("   subscribed a held reference      -> " + listeners.size() + " listeners");
        System.out.println("   remove(held)                     -> returned " + listeners.remove(held)
                + ", now " + listeners.size());

        System.out.println();
        System.out.println("Fix two -- return a handle, which is the one to teach:");
        Publisher publisher = new Publisher();
        Subscription sub = publisher.subscribe(event -> record(auditLog, event));
        System.out.println("   subscribed via Publisher         -> " + publisher.count() + " listener");
        sub.close();
        System.out.println("   sub.close()                      -> " + publisher.count() + " listeners");
        System.out.println();
        System.out.println("The caller never has to hold the lambda, and there is no way to ask for");
        System.out.println("the wrong removal. Correct unsubscription is the only path available.");
    }

    static void record(Object log, String event) { }

    interface Subscription extends AutoCloseable {
        @Override void close();
    }

    static final class Publisher {
        private final List<Consumer<String>> listeners = new ArrayList<>();

        Subscription subscribe(Consumer<String> listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        int count() { return listeners.size(); }
    }

    /* ------------------------------------------------------------------ */

    private final String name = "the enclosing LambdaIdentityDemo";

    /** What `this` means inside each, and why it is a capture problem. */
    void partFive() {
        System.out.println("== part five: what `this` refers to ==");

        WaitlistPolicy lambda = id -> {
            System.out.println("inside a lambda, this     -> " + this.name);
            return 0;
        };
        lambda.rank("S001");

        WaitlistPolicy anonymous = new WaitlistPolicy() {
            public int rank(String id) {
                System.out.println("inside an anon class, this -> " + this.getClass().getName()
                        + ", its own object");
                return 0;
            }
        };
        anonymous.rank("S001");

        System.out.println();
        System.out.println("A lambda has no `this` of its own, so `this` is the enclosing instance.");
        System.out.println("That means a lambda written inside an instance method captures the whole");
        System.out.println("enclosing object, not just the fields it mentions. Subscribe one of those");
        System.out.println("and forget to unsubscribe, and the listener list holds that object and");
        System.out.println("everything it references. This is the capture cost the note warns about,");
        System.out.println("and it is the second half of why the failure above is a leak and not");
        System.out.println("merely a missed callback.");
    }
}
