package registration;

import java.util.ArrayList;
import java.util.List;

/**
 * Answers whether a student may enrol in an offering.
 *
 * This class asks questions and changes nothing. Calling it twice gives the
 * same answer twice, and calling it never enrols anybody.
 */
public class EligibilityChecker {

    public EligibilityResult check(Student student, CourseOffering offering) {
        List<Violation> violations = new ArrayList<>();
        String[][] rows = LegacySisClient.getInstance().fetchGradeRows(student.getId());
        String code = offering.getCourse().getCode();

        if (hasPassed(rows, code)) {
            violations.add(new Violation(RuleId.ALREADY_COMPLETED,
                    "Already completed " + code));
        }
        if (alreadyEnrolled(student, offering)) {
            violations.add(new Violation(RuleId.DUPLICATE_ENROLMENT,
                    "Already enrolled in " + code));
        }
        for (String prereq : offering.getCourse().getPrerequisites()) {
            if (!hasPassed(rows, prereq)) {
                violations.add(new Violation(RuleId.MISSING_PREREQUISITE,
                        "Missing prerequisite: " + prereq));
            }
        }
        Violation clash = timeConflict(student, offering);
        if (clash != null) {
            violations.add(clash);
        }
        Violation overCap = creditCap(student, offering);
        if (overCap != null) {
            violations.add(overCap);
        }
        if (!offering.hasOpenSeats()) {
            violations.add(new Violation(RuleId.SECTION_FULL, "Section is full"));
        }
        return new EligibilityResult(violations);
    }

    private boolean hasPassed(String[][] rows, String courseCode) {
        for (String[] row : rows) {
            if (row[0].equals(courseCode) && Grade.valueOf(row[1]).isPassing()) {
                return true;
            }
        }
        return false;
    }

    private boolean alreadyEnrolled(Student student, CourseOffering offering) {
        for (CourseOffering current : student.getCurrentEnrollments()) {
            if (current.getCrn().equals(offering.getCrn())) {
                return true;
            }
        }
        return false;
    }

    private Violation timeConflict(Student student, CourseOffering offering) {
        for (CourseOffering current : student.getCurrentEnrollments()) {
            if (current.getCrn().equals(offering.getCrn())) {
                continue;   // a section does not clash with itself
            }
            for (MeetingTime existing : current.getMeetings()) {
                for (MeetingTime proposed : offering.getMeetings()) {
                    if (existing.overlaps(proposed)) {
                        return new Violation(RuleId.TIME_CONFLICT,
                                "Time conflict with " + current.getCourse().getCode());
                    }
                }
            }
        }
        return null;
    }

    private Violation creditCap(Student student, CourseOffering offering) {
        int cap = Config.getInt("max_credits", 18);
        int adding = alreadyEnrolled(student, offering) ? 0 : offering.getCourse().getCredits();
        int total = student.getCurrentCredits() + adding;
        if (total > cap) {
            return new Violation(RuleId.CREDIT_CAP,
                    "Would total " + total + " credits, limit is " + cap);
        }
        return null;
    }
}
