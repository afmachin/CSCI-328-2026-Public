package registration;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        LegacySisClient sis = LegacySisClient.getInstance();
        sis.seed("S001", "CS101", "B", "2025FA");
        sis.seed("S002", "CS101", "F", "2025FA");

        Course cs101 = new Course("CS101", "Intro to Programming", 4);
        Course cs201 = new Course("CS201", "Data Structures", 4);
        cs201.getPrerequisites().add("CS101");
        Course cs301 = new Course("CS301", "Object Oriented Design", 3);
        cs301.getPrerequisites().add("CS201");

        CourseOffering ds = new CourseOffering("10422", cs201, 2);
        ds.getMeetings().add(new MeetingTime("MON", 540, 630));
        ds.getMeetings().add(new MeetingTime("WED", 540, 630));

        CourseOffering ood = new CourseOffering("10515", cs301, 30);
        ood.getMeetings().add(new MeetingTime("MON", 600, 690));

        Student alice = new Student("S001", "Alice Reyes", "UNDERGRADUATE");
        Student ben = new Student("S002", "Ben Okafor", "UNDERGRADUATE");

        RegistrationService service = new RegistrationService();

        System.out.println(service.describeResult(alice, ds));
        System.out.println(service.describeResult(ben, ds));
        System.out.println(service.describeResult(alice, ood));

        System.out.println();
        System.out.println("Alice is enrolled in: " + Arrays.toString(
                alice.getCurrentEnrollments().stream()
                     .map(o -> o.getCourse().getCode()).toArray()));

        Roster roster = new Roster("10422", 2);
        roster.addStudent(alice);;
        System.out.println();
        System.out.print(service.formatRosterForAdvising(roster));
    }
}
