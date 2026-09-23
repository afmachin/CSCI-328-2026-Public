package registration;

import java.util.Arrays;
import java.util.List;

public class Check {

    public static void main(String[] args) {
        CourseOffering crowded = new CourseOffering("10001",
                new Course("CS101", "Intro to Programming", 3), 2);
        CourseOffering routine = new CourseOffering("10002",
                new Course("ENG101", "Writing", 3), 2);

        Student alice = new Student("S001", "Alice Reyes", "UNDERGRADUATE");
        Student ben = new Student("S002", "Ben Okafor", "UNDERGRADUATE");
        Student cara = new Student("S003", "Cara Chen", "UNDERGRADUATE");
        RegistrationService service = new RegistrationService();

        System.out.println("Registration and drop calls:");
        System.out.println("1. register Alice: "
                + service.register(alice, crowded));
        System.out.println("2. register Alice again: "
                + service.register(alice, crowded));
        System.out.println("3. register Alice when full: "
                + service.register(alice, crowded));
        System.out.println("4. register Alice when full again: "
                + service.register(alice, crowded));
        System.out.println("5. register Ben: "
                + service.register(ben, routine));
        System.out.println("6. register Cara: "
                + service.register(cara, routine));

        service.drop(ben, routine);
        System.out.println("7. drop Ben: done");
        service.drop(cara, routine);
        System.out.println("8. drop Cara: done");

        List<Student> students = Arrays.asList(alice, ben, cara);
        List<CourseOffering> offerings = Arrays.asList(crowded, routine);

        System.out.println();
        print("1. Enrollment counts",
                Invariants.checkEnrollmentCounts(students, offerings));
        print("2. Credit limits",
                Invariants.checkCreditLimits(students));
        print("3. Waitlists",
                Invariants.checkWaitlists(
                        students, offerings, service.getWaitlist()));
        print("4. Capacity",
                Invariants.checkCapacity(offerings));
    }

    private static void print(String heading, List<String> violations) {
        System.out.println(heading + ":");
        if (violations.isEmpty()) {
            System.out.println("  none");
        }
        for (String violation : violations) {
            System.out.println("  - " + violation);
        }
    }
}
