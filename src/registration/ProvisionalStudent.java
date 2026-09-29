package registration;

import java.util.ArrayList;
import java.util.List;

/** Admission is provisional until the transcript arrives, so enrolments are
 * held rather than taken — "subject to confirmation," as the letter says. */
public class ProvisionalStudent extends Student {
    private final List<CourseOffering> pending = new ArrayList<CourseOffering>();

    public ProvisionalStudent(String id, String name) {
        super(id, name, "UNDERGRADUATE");
    }

    @Override
    public void enroll(CourseOffering offering) {
        pending.add(offering);
    }

    public List<CourseOffering> getPending() {
        return pending;
    }
}