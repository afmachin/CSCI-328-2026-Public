public final class Rules {
    private Rules() { }

    public static RuleOutcome prerequisites(EnrollmentRequest request) {
        return RuleOutcome.PASS;
    }

    public static RuleOutcome seatsAvailable(EnrollmentRequest request) {
        throw new IllegalStateException("seat count unavailable");
    }

    public static RuleOutcome creditLimit(EnrollmentRequest request) {
        return RuleOutcome.PASS;
    }
}
