package registration;

import java.util.ArrayList;
import java.util.List;

public class Catalog {

    public static void main(String[] args) {
        attack1();
        attack2();
        attack3();
    }

    private static Course createCourse(List<String> prerequisites) {
        return new Course("CS201", "Data Structures", 4, prerequisites);
    }

    private static Student passingStudent(String id, String name) {
        LegacySisClient.getInstance().seed(id, "CS101", "B", "2025FA");
        return new Student(id, name, "UNDERGRADUATE");
    }

    private static void syncMath150(Course course) {
        course.getPrerequisites().add("MATH150");
    }

    private static void attack1() {
        System.out.println("Attack 1: through the getter");
        ArrayList<String> prerequisites = new ArrayList<>();
        prerequisites.add("CS101");
        Course course = createCourse(prerequisites);
        CourseOffering section = new CourseOffering("20101", course, 2);
        Student first = passingStudent("CG001", "Getter First");
        Student second = passingStudent("CG002", "Getter Second");
        RegistrationService service = new RegistrationService();

        System.out.println(service.describeResult(first, section));
        try {
            syncMath150(course);
            System.out.println("Getter sync completed.");
        } catch (UnsupportedOperationException ex) {
            System.out.println("Getter sync blocked: UnsupportedOperationException");
        }
        System.out.println(service.describeResult(second, section));
        System.out.println();
    }

    private static void attack2() {
        System.out.println("Attack 2: through the held list");
        ArrayList<String> heldList = new ArrayList<>();
        heldList.add("CS101");
        Course course = createCourse(heldList);
        CourseOffering section = new CourseOffering("20102", course, 2);
        Student first = passingStudent("CA001", "Alias First");
        Student second = passingStudent("CA002", "Alias Second");
        RegistrationService service = new RegistrationService();

        System.out.println(service.describeResult(first, section));
        heldList.add("MATH150");
        System.out.println(service.describeResult(second, section));
        System.out.println("Course prerequisites: " + course.getPrerequisites());
        System.out.println();
    }

    private static void attack3() {
        System.out.println("Attack 3: null prerequisite");
        ArrayList<String> prerequisites = new ArrayList<>();
        prerequisites.add("CS101");
        prerequisites.add(null);
        Student student = passingStudent("CN001", "Null Student");

        Course course;
        try {
            course = createCourse(prerequisites);
        } catch (NullPointerException ex) {
            System.out.println("Construction failed: NullPointerException");
            System.out.println("Advising screen: no course created; registration not attempted.");
            return;
        }
        System.out.println("Construction succeeded.");
        CourseOffering section = new CourseOffering("20103", course, 2);
        RegistrationService service = new RegistrationService();
        System.out.println(service.describeResult(student, section));
    }
}
