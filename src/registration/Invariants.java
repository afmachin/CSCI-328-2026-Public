package registration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Invariants {

    public static List<String> checkEnrollmentCounts(
            List<Student> students, List<CourseOffering> offerings) {
        List<String> violations = new ArrayList<>();

        for (CourseOffering offering : offerings) {
            Set<String> studentIds = new HashSet<>();
            for (Student student : students) {
                if (student.getCurrentEnrollments().contains(offering)) {
                    studentIds.add(student.getId());
                }
            }

            if (offering.getEnrolled() != studentIds.size()) {
                violations.add("Offering " + offering.getCrn()
                        + ": enrolled count is " + offering.getEnrolled()
                        + ", but " + studentIds.size()
                        + " distinct student(s) are enrolled.");
            }
        }
        return violations;
    }

    public static List<String> checkCreditLimits(List<Student> students) {
        List<String> violations = new ArrayList<>();
        int maxCredits = Config.getInt("max_credits", 18);

        for (Student student : students) {
            if (student.getCurrentCredits() > maxCredits) {
                violations.add("Student " + student.getId() + ": "
                        + student.getCurrentCredits()
                        + " credits exceeds max_credits " + maxCredits + ".");
            }
        }
        return violations;
    }

    public static List<String> checkWaitlists(
            List<Student> students, List<CourseOffering> offerings,
            WaitlistManager manager) {
        List<String> violations = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry
                : manager.getByOffering().entrySet()) {
            String crn = entry.getKey();
            Set<String> seen = new LinkedHashSet<>();
            Set<String> reportedDuplicates = new HashSet<>();

            for (String studentId : entry.getValue()) {
                if (!seen.add(studentId)
                        && reportedDuplicates.add(studentId)) {
                    violations.add("Offering " + crn + " waitlist: student "
                            + studentId + " appears more than once.");
                }
            }

            for (String studentId : seen) {
                for (CourseOffering offering : offerings) {
                    if (!crn.equals(offering.getCrn())) {
                        continue;
                    }
                    for (Student student : students) {
                        if (studentId.equals(student.getId())
                                && student.getCurrentEnrollments()
                                        .contains(offering)) {
                            violations.add("Offering " + crn
                                    + " waitlist: student " + studentId
                                    + " is already enrolled.");
                        }
                    }
                }
            }
        }
        return violations;
    }

    public static List<String> checkCapacity(List<CourseOffering> offerings) {
        List<String> violations = new ArrayList<>();
        for (CourseOffering offering : offerings) {
            if (offering.getEnrolled() > offering.getCapacity()) {
                violations.add("Offering " + offering.getCrn()
                        + ": enrolled count " + offering.getEnrolled()
                        + " exceeds capacity " + offering.getCapacity() + ".");
            }
        }
        return violations;
    }
}
