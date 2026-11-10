package registration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SUBMISSION C
 *
 * Developer note included with the submission:
 * "Each rule is its own class behind a common interface, so the checker doesn't
 *  know anything about the individual rules and all failures get collected
 *  instead of stopping at the first one."
 */
public final class EnrollmentChecker {

    private final List<RuleCheck> checks;

    public EnrollmentChecker() {
        this.checks = List.of(
                new DuplicateCheck(),
                new PrerequisiteCheck(),
                new CapacityCheck(),
                new ScheduleCheck(),
                new CreditLimitCheck());
    }

    public EnrollmentDecision check(Student student, CourseOffering offering) {
        List<Violation> violations = new ArrayList<>();
        for (RuleCheck check : checks) {
            String message = check.violationOrNull(student, offering);
            if (message != null) {
                violations.add(new Violation(check.code(), message));
            }
        }
        return new EnrollmentDecision(violations);
    }
}

interface RuleCheck {
    String code();

    /** Returns a description of the violation, or null if the rule passes. */
    String violationOrNull(Student student, CourseOffering offering);
}

final class Violation {

    private final String code;
    private final String message;

    Violation(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }

    public String getMessage() { return message; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Violation)) return false;
        Violation that = (Violation) other;
        return code.equals(that.code) && message.equals(that.message);
    }

    @Override
    public String toString() { return code + ": " + message; }
}

final class EnrollmentDecision {

    private final List<Violation> violations;

    EnrollmentDecision(List<Violation> violations) {
        this.violations = violations;
    }

    public boolean isAllowed() { return violations.isEmpty(); }

    public List<Violation> getViolations() { return violations; }

    /** The advising screen shows each distinct problem once. */
    public Set<Violation> distinctViolations() {
        return new HashSet<>(violations);
    }
}

/* ------------------------------------------------------------------ */

final class DuplicateCheck implements RuleCheck {
    public String code() { return "DUPLICATE"; }

    public String violationOrNull(Student student, CourseOffering offering) {
        for (TranscriptEntry entry : student.transcript()) {
            if (entry.courseCode().equals(offering.course().code())
                    && entry.grade().isPassing()) {
                return "Already completed with a grade of " + entry.grade();
            }
        }
        return null;
    }
}

final class PrerequisiteCheck implements RuleCheck {
    public String code() { return "PREREQUISITE"; }

    public String violationOrNull(Student student, CourseOffering offering) {
        List<String> missing = new ArrayList<>();
        for (String prereq : offering.course().prerequisiteCodes()) {
            boolean satisfied = false;
            for (TranscriptEntry entry : student.transcript()) {
                if (entry.courseCode().equals(prereq) && entry.grade().isPassing()) {
                    satisfied = true;
                }
            }
            if (!satisfied) missing.add(prereq);
        }
        return missing.isEmpty() ? null : "Missing prerequisites: " + String.join(", ", missing);
    }
}

final class CapacityCheck implements RuleCheck {
    public String code() { return "CAPACITY"; }

    public String violationOrNull(Student student, CourseOffering offering) {
        return offering.hasOpenSeats() ? null : "Section " + offering.crn() + " is full";
    }
}

final class ScheduleCheck implements RuleCheck {
    public String code() { return "SCHEDULE"; }

    public String violationOrNull(Student student, CourseOffering offering) {
        // Sort so the earliest conflicting course is reported first.
        student.currentEnrollments().sort(Comparator.comparing(o -> o.course().code()));

        for (CourseOffering current : student.currentEnrollments()) {
            for (MeetingTime existing : current.meetings()) {
                for (MeetingTime proposed : offering.meetings()) {
                    if (existing.overlaps(proposed)) {
                        return "Conflicts with " + current.course().code();
                    }
                }
            }
        }
        return null;
    }
}

final class CreditLimitCheck implements RuleCheck {

    private static final int MAX_CREDITS = 18;

    public String code() { return "CREDIT_LIMIT"; }

    public String violationOrNull(Student student, CourseOffering offering) {
        int total = offering.course().credits();
        for (CourseOffering current : student.currentEnrollments()) {
            total += current.course().credits();
        }
        return total > MAX_CREDITS
                ? "Total would be " + total + " credits, limit is " + MAX_CREDITS
                : null;
    }
}
