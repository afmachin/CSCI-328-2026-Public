package registration;

import java.util.ArrayList;
import java.util.List;

public class RegistrationService {

    private List<String> lastErrors = new ArrayList<String>();

    private WaitlistManager waitlist = new WaitlistManager();
    private AuditLog audit = new AuditLog();
    private NotificationGateway notifications = new NotificationGateway();

    public List<String> getLastErrors() {
        return lastErrors;
    }

    public WaitlistManager getWaitlist() {
        return waitlist;
    }

    public boolean register(Student student, CourseOffering offering) {
        lastErrors.clear();

        String[][] rows = LegacySisClient.getInstance().fetchGradeRows(student.getId());

        for (int i = 0; i < rows.length; i++) {
            if (rows[i][0].equals(offering.getCourse().getCode())
                    && Grade.valueOf(rows[i][1]).isPassing()) {
                lastErrors.add("Already completed " + offering.getCourse().getCode());
                return false;
            }
        }

        for (String prereq : offering.getCourse().getPrerequisites()) {
            boolean satisfied = false;
            for (int i = 0; i < rows.length; i++) {
                if (rows[i][0].equals(prereq) && Grade.valueOf(rows[i][1]).isPassing()) {
                    satisfied = true;
                }
            }
            if (!satisfied) {
                lastErrors.add("Missing prerequisite: " + prereq);
                return false;
            }
        }

        for (CourseOffering current : student.getCurrentEnrollments()) {
            for (MeetingTime existing : current.getMeetings()) {
                for (MeetingTime proposed : offering.getMeetings()) {
                    if (existing.overlaps(proposed)) {
                        lastErrors.add("Time conflict with " + current.getCourse().getCode());
                        return false;
                    }
                }
            }
        }

        int maxCredits = Config.getInt("max_credits", 18);
        int total = student.getCurrentCredits() + offering.getCourse().getCredits();
        if (total > maxCredits) {
            lastErrors.add("Would total " + total + " credits, limit is " + maxCredits);
            return false;
        }

        if (!offering.hasOpenSeats()) {
            if (Config.settings.get("waitlist_enabled").equals("true")) {
                waitlist.add(offering.getCrn(), student.getId());
                audit.record("WAITLISTED", student.getId() + " -> " + offering.getCrn());
                lastErrors.add("Section full; added to waitlist at position "
                        + waitlist.positionOf(offering.getCrn(), student.getId()));
            } else {
                lastErrors.add("Section is full");
            }
            return false;
        }

        if (student instanceof GraduateStudent
                && !((GraduateStudent) student).hasAdvisorApproval()) {
            lastErrors.add("Graduate students require advisor approval before enrolling");
            return false;
        }

        student.enroll(offering);
        audit.record("ENROLLED", student.getId() + " -> " + offering.getCrn());
        notifications.send(student.getId(), "Enrollment confirmed",
                "You are enrolled in " + offering.getCourse().getCode() + ".");
        return true;
    }

    public void drop(Student student, CourseOffering offering) {
        student.getCurrentEnrollments().remove(offering);
        offering.setEnrolled(offering.getEnrolled() - 1);
        audit.record("DROPPED", student.getId() + " -> " + offering.getCrn());

        String promoted = waitlist.promoteNext(offering.getCrn());
        if (promoted != null) {
            String template = Config.settings.get("promotion_message");
            String body = template.replace("{course}", offering.getCourse().getCode());
            notifications.send(promoted, "Seat available", body);
        }
    }

    public String describeResult(Student student, CourseOffering offering) {
        if (register(student, offering)) {
            return student.getName() + " registered for " + offering.getCourse().getCode();
        }
        String out = student.getName() + " could not register:";
        for (String error : lastErrors) {
            out = out + "\n  - " + error;
        }
        return out;
    }

    public String formatRosterForAdvising(Roster roster) {
        StringBuilder out = new StringBuilder("Roster " + roster.getCrn() + ":\n");
        for (Student s : roster.getStudents().values()) {
            out.append("  ").append(s.getId()).append(" ").append(s.getName())
               .append(" (").append(s.getCurrentCredits()).append(" cr)\n");
        }
        return out.toString();
    }

    public String exportRosterCsv(Roster roster) {
        StringBuilder out = new StringBuilder("student_id,name,credits\n");
        for (Student s : roster.getStudents().values()) {
            out.append(s.getId()).append(",").append(s.getName()).append(",")
               .append(s.getCurrentCredits()).append("\n");
        }
        return out.toString();
    }
}
