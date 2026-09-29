package registration;

import java.util.ArrayList;
import java.util.List;

/** A visiting student's record is read by the home institution's reporting job,
 * so this class does not hand out its live enrolment list — Week 1's lesson. */
public class VisitingStudent extends Student {
    public VisitingStudent(String id, String name) {
        super(id, name, "UNDERGRADUATE");
    }

    @Override
    public List<CourseOffering> getCurrentEnrollments() {
        return new ArrayList<CourseOffering>(super.getCurrentEnrollments());
    }
}