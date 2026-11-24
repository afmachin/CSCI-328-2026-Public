@FunctionalInterface
public interface EligibilityRule {
    RuleOutcome evaluate(EnrollmentRequest request);

    default EligibilityRule and(EligibilityRule next) {
        return request -> {
            RuleOutcome first = evaluate(request);
            return first.ok() ? next.evaluate(request) : first;
        };
    }
}
