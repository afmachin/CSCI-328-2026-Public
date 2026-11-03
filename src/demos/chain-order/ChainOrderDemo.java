import java.util.List;
import java.util.Optional;

/**
 * Chain of Responsibility: the handler order IS the policy, and it is written down
 * nowhere except the order somebody built the list in.
 *
 * Three real authorities over a registration override, one request, two assemblies.
 * Nothing about the handlers changes between the two runs.
 */
public class ChainOrderDemo {

    record OverrideRequest(String studentId, String crn, String kind, int amount) { }

    record Approval(String grantedBy, String because) { }

    @FunctionalInterface
    interface OverrideHandler {
        Optional<Approval> tryHandle(OverrideRequest r);
        default String name() { return getClass().getSimpleName(); }
    }

    // The department chair can waive a prerequisite, and nothing else.
    static final OverrideHandler CHAIR = r ->
            r.kind().equals("PREREQ")
                    ? Optional.of(new Approval("department chair", "prerequisite waiver"))
                    : Optional.empty();

    // The dean can allow a credit overload, but only up to three credits over.
    static final OverrideHandler DEAN = r ->
            r.kind().equals("CREDIT_LIMIT") && r.amount() <= 3
                    ? Optional.of(new Approval("dean", "overload of " + r.amount() + " credits"))
                    : Optional.empty();

    // The registrar can do anything. That is exactly why position matters.
    static final OverrideHandler REGISTRAR = r ->
            Optional.of(new Approval("registrar", "registrar authority"));

    static Optional<Approval> handle(List<OverrideHandler> chain, OverrideRequest r) {
        return chain.stream()
                .map(h -> h.tryHandle(r))
                .flatMap(Optional::stream)
                .findFirst();
    }

    public static void main(String[] args) {
        OverrideRequest request = new OverrideRequest("S7", "10515", "CREDIT_LIMIT", 2);

        List<OverrideHandler> asIntended = List.of(CHAIR, DEAN, REGISTRAR);
        List<OverrideHandler> reordered  = List.of(CHAIR, REGISTRAR, DEAN);

        System.out.println("one request: " + request.kind() + ", " + request.amount() + " credits over");
        System.out.println();

        System.out.println("chain built chair -> dean -> registrar");
        System.out.println("  granted by: " + handle(asIntended, request).get().grantedBy());

        System.out.println("chain built chair -> registrar -> dean");
        System.out.println("  granted by: " + handle(reordered, request).get().grantedBy());

        System.out.println();
        System.out.println("Same three handlers. Same request. Different authority.");
        System.out.println("Nothing in any handler changed, and no handler can tell you which");
        System.out.println("list it is in. The institutional rule lives in the order alone.");
    }
}
