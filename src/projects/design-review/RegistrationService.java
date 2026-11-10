package registration;

import java.util.ArrayList;
import java.util.List;

/**
 * SUBMISSION A
 *
 * Developer note included with the submission:
 * "I kept it simple and didn't add classes I didn't need. Everything the
 *  registrar needs to check is in one place so it's easy to find."
 */
public class RegistrationService {

    private static final int MAX_CREDITS = 18;

    private List<String> lastErrors = new ArrayList<>();

    public List<String> getLastErrors() {
        return lastErrors;
    }

    public boolean canEnroll(Student student, CourseOffering offering) {
        lastErrors.clear();

        // Rule 4: already passed the course
        for (TranscriptEntry entry : student.transcript()) {
            if (entry.courseCode().equals(offering.course().code())
                    && entry.grade().isPassing()) {
                lastErrors.add("Student has already passed " + offering.course().code());
                return false;
            }
        }

        // Rule 1: prerequisites
        for (String prereq : offering.course().prerequisiteCodes()) {
            boolean satisfied = false;
            for (TranscriptEntry entry : student.transcript()) {
                if (entry.courseCode().equals(prereq) && entry.grade().isPassing()) {
                    satisfied = true;
                }
            }
            if (!satisfied) {
                lastErrors.add("Missing prerequisite: " + prereq);
                return false;
            }
        }

        // Rule 2: seats
        if (!offering.hasOpenSeats()) {
            lastErrors.add("Section " + offering.crn() + " is full");
            return false;
        }

        // Rule 3: time conflicts
        for (CourseOffering current : student.currentEnrollments()) {
            for (MeetingTime existing : current.meetings()) {
                for (MeetingTime proposed : offering.meetings()) {
                    if (existing.overlaps(proposed)) {
                        lastErrors.add("Time conflict with " + current.course().code());
                        return false;
                    }
                }
            }
        }

        // Rule 5: credit cap
        int totalCredits = offering.course().credits();
        for (CourseOffering current : student.currentEnrollments()) {
            totalCredits = totalCredits + current.course().credits();
        }
        if (totalCredits > MAX_CREDITS) {
            lastErrors.add("Enrolling would exceed the " + MAX_CREDITS + " credit limit");
            return false;
        }

        student.currentEnrollments().add(offering);
        return true;
    }

    public String describeResult(Student student, CourseOffering offering) {
        if (canEnroll(student, offering)) {
            return student.name() + " may enroll in " + offering.course().code();
        }
        String out = student.name() + " may not enroll:";
        for (String error : lastErrors) {
            out = out + "\n  - " + error;
        }
        return out;
    }
}
