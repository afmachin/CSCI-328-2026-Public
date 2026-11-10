package registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SUBMISSION D
 *
 * Developer note included with the submission:
 * "A failed rule carries a typed reason rather than a string, so callers can
 *  react to the specific failure. Turning reasons into English is a separate
 *  concern from deciding eligibility."
 */
public final class EligibilityRules {

    private static final int MAX_CREDITS = 18;

    private EligibilityRules() {}

    public static List<EligibilityRule> standard() {
        return List.of(
                EligibilityRules::notAlreadyCompleted,
                EligibilityRules::prerequisitesMet,
                EligibilityRules::seatsAvailable,
                EligibilityRules::noScheduleConflict,
                EligibilityRules::withinCreditLimit);
    }

    static RuleOutcome notAlreadyCompleted(EnrollmentRequest request) {
        for (TranscriptEntry entry : request.student().transcript()) {
            if (entry.courseCode().equals(request.offering().course().code())
                    && entry.grade().isPassing()) {
                return new RuleOutcome.Violated("DUPLICATE",
                        new ViolationReason.AlreadyCompleted(entry.courseCode(), entry.grade()));
            }
        }
        return new RuleOutcome.Satisfied("DUPLICATE");
    }

    static RuleOutcome prerequisitesMet(EnrollmentRequest request) {
        for (String prereq : request.offering().course().prerequisiteCodes()) {
            if (!request.hasPassed(prereq)) {
                return new RuleOutcome.Violated("PREREQUISITE",
                        new ViolationReason.MissingPrerequisite(prereq));
            }
        }
        return new RuleOutcome.Satisfied("PREREQUISITE");
    }

    static RuleOutcome seatsAvailable(EnrollmentRequest request) {
        CourseOffering offering = request.offering();
        return offering.hasOpenSeats()
                ? new RuleOutcome.Satisfied("CAPACITY")
                : new RuleOutcome.Violated("CAPACITY",
                        new ViolationReason.SectionFull(offering.capacity()));
    }

    static RuleOutcome noScheduleConflict(EnrollmentRequest request) {
        for (CourseOffering current : request.student().currentEnrollments()) {
            for (MeetingTime existing : current.meetings()) {
                for (MeetingTime proposed : request.offering().meetings()) {
                    if (existing.overlaps(proposed)) {
                        return new RuleOutcome.Violated("SCHEDULE",
                                new ViolationReason.ScheduleConflict(current.crn()));
                    }
                }
            }
        }
        return new RuleOutcome.Satisfied("SCHEDULE");
    }

    static RuleOutcome withinCreditLimit(EnrollmentRequest request) {
        int requested = request.creditsAfterEnrolling();
        return requested <= MAX_CREDITS
                ? new RuleOutcome.Satisfied("CREDIT_LIMIT")
                : new RuleOutcome.Violated("CREDIT_LIMIT",
                        new ViolationReason.CreditLimitExceeded(requested, MAX_CREDITS));
    }
}

@FunctionalInterface
interface EligibilityRule {
    RuleOutcome evaluate(EnrollmentRequest request);
}

record EnrollmentRequest(Student student, CourseOffering offering) {

    EnrollmentRequest {
        Objects.requireNonNull(student, "student");
        Objects.requireNonNull(offering, "offering");
    }

    boolean hasPassed(String courseCode) {
        return student.transcript().stream()
                .anyMatch(e -> e.courseCode().equals(courseCode) && e.grade().isPassing());
    }

    int creditsAfterEnrolling() {
        return offering.course().credits()
                + student.currentEnrollments().stream()
                        .mapToInt(o -> o.course().credits())
                        .sum();
    }
}

sealed interface RuleOutcome {
    String ruleId();

    record Satisfied(String ruleId) implements RuleOutcome {}

    record Violated(String ruleId, ViolationReason reason) implements RuleOutcome {}
}

sealed interface ViolationReason {
    record MissingPrerequisite(String courseCode) implements ViolationReason {}
    record SectionFull(int capacity) implements ViolationReason {}
    record ScheduleConflict(String conflictingCrn) implements ViolationReason {}
    record CreditLimitExceeded(int requested, int maximum) implements ViolationReason {}
    record AlreadyCompleted(String courseCode, Grade grade) implements ViolationReason {}
}

record EligibilityDecision(List<RuleOutcome> outcomes) {

    EligibilityDecision {
        outcomes = List.copyOf(outcomes);
    }

    static EligibilityDecision evaluate(EnrollmentRequest request,
                                        List<EligibilityRule> rules) {
        List<RuleOutcome> results = new ArrayList<>();
        for (EligibilityRule rule : rules) {
            results.add(rule.evaluate(request));
        }
        return new EligibilityDecision(results);
    }

    boolean allowed() {
        return violations().isEmpty();
    }

    List<RuleOutcome.Violated> violations() {
        return outcomes.stream()
                .filter(RuleOutcome.Violated.class::isInstance)
                .map(RuleOutcome.Violated.class::cast)
                .toList();
    }
}

final class ViolationMessages {

    private ViolationMessages() {}

    static String render(ViolationReason reason) {
        return switch (reason) {
            case ViolationReason.MissingPrerequisite r ->
                    "Missing prerequisite: " + r.courseCode();
            case ViolationReason.SectionFull r ->
                    "Section is full (" + r.capacity() + " seats)";
            case ViolationReason.ScheduleConflict r ->
                    "Time conflict with section " + r.conflictingCrn();
            case ViolationReason.CreditLimitExceeded r ->
                    "Would total " + r.requested() + " credits; limit is " + r.maximum();
            case ViolationReason.AlreadyCompleted r ->
                    "Already completed " + r.courseCode() + " with a grade of " + r.grade();
        };
    }
}
