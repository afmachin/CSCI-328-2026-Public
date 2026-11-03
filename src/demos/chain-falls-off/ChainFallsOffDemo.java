import java.util.List;
import java.util.Optional;

/**
 * Chain of Responsibility: what happens when nothing claims the request.
 *
 * The chain itself is not wrong here. The chain returns an empty Optional, which is
 * an honest answer. The failure is that "nobody had authority" and "the request was
 * denied" are different facts, and an empty Optional is the same value for both --
 * so the caller that forgets to distinguish them ships a decision nobody made.
 */
public class ChainFallsOffDemo {

    record OverrideRequest(String studentId, String kind, int amount) { }
    record Approval(String grantedBy) { }

    static class NoAuthorityException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        NoAuthorityException(OverrideRequest r) {
            super("no handler has authority over " + r.kind() + " for " + r.studentId());
        }
    }

    @FunctionalInterface
    interface OverrideHandler { Optional<Approval> tryHandle(OverrideRequest r); }

    static final OverrideHandler CHAIR = r ->
            r.kind().equals("PREREQ") ? Optional.of(new Approval("department chair")) : Optional.empty();
    static final OverrideHandler DEAN = r ->
            r.kind().equals("CREDIT_LIMIT") && r.amount() <= 3
                    ? Optional.of(new Approval("dean")) : Optional.empty();

    static final List<OverrideHandler> CHAIN = List.of(CHAIR, DEAN);

    static Optional<Approval> handle(OverrideRequest r) {
        return CHAIN.stream().map(h -> h.tryHandle(r)).flatMap(Optional::stream).findFirst();
    }

    public static void main(String[] args) {
        // Nobody in this chain has authority over a time conflict.
        OverrideRequest unclaimed = new OverrideRequest("S7", "TIME_CONFLICT", 0);

        System.out.println("request: " + unclaimed.kind());
        System.out.println("chain:   chair, dean");
        System.out.println("result:  " + handle(unclaimed));
        System.out.println();

        System.out.println("--- caller A: treats empty as 'denied' ---");
        boolean approved = handle(unclaimed).isPresent();
        System.out.println("approved = " + approved);
        System.out.println("tells the student: " + (approved ? "override granted" : "override denied"));
        System.out.println("...which is a denial nobody issued. No chair, no dean and no");
        System.out.println("registrar ever saw this request.");
        System.out.println();

        System.out.println("--- caller B: treats empty as 'not my decision to make' ---");
        try {
            handle(unclaimed).orElseThrow(() -> new NoAuthorityException(unclaimed));
        } catch (NoAuthorityException e) {
            System.out.println("NoAuthorityException: " + e.getMessage());
            System.out.println("...which is the truth, and is now somebody's job.");
        }
        System.out.println();
        System.out.println("Same chain, same empty Optional. The decision is the caller's,");
        System.out.println("and falling off the end makes it by default.");
    }
}
