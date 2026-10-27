import java.util.HashSet;
import java.util.Set;

/**
 * The costs of wrapping
 *
 *   cd demos/wrapper-identity
 *   javac WrapperDemo.java && java WrapperDemo
 *
 * The recorded output is in output.txt beside this file.
 *
 * Part three prints a real stack trace through four wrappers. Read it carefully: the
 * order is the reverse of the order you assembled them in.
 */
public class WrapperDemo {

    interface Rule {
        String evaluate(String studentId);
    }

    record CreditLimitRule(int max) implements Rule {
        public String evaluate(String studentId) {
            throw new IllegalStateException("the real work, for " + studentId);
        }
    }

    record AuditedRule(Rule inner) implements Rule {
        public String evaluate(String s) { return inner.evaluate(s); }
    }
    record TimedRule(Rule inner) implements Rule {
        public String evaluate(String s) { return inner.evaluate(s); }
    }
    record CachedRule(Rule inner) implements Rule {
        public String evaluate(String s) { return inner.evaluate(s); }
    }
    record RetriedRule(Rule inner) implements Rule {
        public String evaluate(String s) { return inner.evaluate(s); }
    }

    public static void main(String[] args) {
        partOne();
        System.out.println();
        partTwo();
        System.out.println();
        partThree();
    }

    /** Identity: every reference-based check a caller might make. */
    static void partOne() {
        System.out.println("== part one: identity ==");

        Rule rule = new CreditLimitRule(18);
        Rule wrapped = new AuditedRule(rule);

        System.out.println("wrapped == rule                       -> " + (wrapped == rule));
        System.out.println("wrapped.getClass().getSimpleName()    -> " + wrapped.getClass().getSimpleName());
        System.out.println("rule.getClass().getSimpleName()       -> " + rule.getClass().getSimpleName());
        System.out.println("wrapped instanceof CreditLimitRule    -> " + (wrapped instanceof CreditLimitRule));
        System.out.println();
        System.out.println("The last line is the one that breaks working code: something wanted to");
        System.out.println("skip credit rules for graduate students, and the check now says no.");
    }

    /** Equality: what records give you, and what they do not. */
    static void partTwo() {
        System.out.println("== part two: equality ==");

        Rule rule = new CreditLimitRule(18);
        Rule w1 = new AuditedRule(rule);
        Rule w2 = new AuditedRule(new CreditLimitRule(18));

        System.out.println("w1 == w2                 -> " + (w1 == w2));
        System.out.println("w1.equals(w2)            -> " + w1.equals(w2)
                + "    records: equal contents, equal wrappers");
        System.out.println("w1.equals(rule)          -> " + w1.equals(rule));
        System.out.println("rule.equals(w1)          -> " + rule.equals(w1));
        System.out.println();
        System.out.println("Both of those last two are false, in both directions. That is ordinary");
        System.out.println("inequality, not the symmetry break note 03 warned about: a wrapper is");
        System.out.println("simply not the thing it wraps. The consequence is still real --");

        Set<Rule> set = new HashSet<>();
        set.add(rule);
        set.add(w1);
        System.out.println("set containing the rule and its wrapper -> size " + set.size()
                + "   (one rule, two entries)");
    }

    /** Stack depth, and the direction you have to read it in. */
    static void partThree() {
        System.out.println("== part three: four wrappers, one stack trace ==");
        System.out.println("assembled as: Retried( Timed( Cached( Audited( CreditLimitRule ))))");
        System.out.println();

        Rule stack = new RetriedRule(new TimedRule(new CachedRule(
                new AuditedRule(new CreditLimitRule(18)))));

        try {
            stack.evaluate("S001");
        } catch (IllegalStateException e) {
            for (StackTraceElement frame : e.getStackTrace()) {
                System.out.println("   at " + frame);
            }
            System.out.println();
            System.out.println("Four wrapper frames sit between main and the work, every one of them");
            System.out.println("named `evaluate`. And the trace runs inside-out, which is the reverse");
            System.out.println("of how you read the expression: you wrote Retried first, and it is the");
            System.out.println("last wrapper in the trace, just above main. Audited is the one you");
            System.out.println("wrote last and it reports first, because it is nearest the work.");
            System.out.println();
            System.out.println("Nothing in the trace tells you which layer cached, which timed or which");
            System.out.println("retried. Only the file names do, and they are named for concerns rather");
            System.out.println("than for rules.");
        }
    }
}
