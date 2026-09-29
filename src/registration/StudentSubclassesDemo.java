package registration;

public class StudentSubclassesDemo {
    public static void main(String[] args) {
        RegistrationService service = new RegistrationService();

        System.out.println("ExchangeStudent");
        LegacySisClient.getInstance().seed("E001", "CS101", "B", "2026FA");
        Course advanced = new Course("CS201", "Data Structures", 4);
        advanced.getPrerequisites().add("CS101");
        CourseOffering exchangeSection = new CourseOffering("20001", advanced, 2);
        ExchangeStudent exchange = new ExchangeStudent("E001", "Exchange", "2026FA");
        System.out.println("getId=" + exchange.getId());
        System.out.println("registered=" + service.register(exchange, exchangeSection));
        System.out.println("errors=" + service.getLastErrors());

        System.out.println("\nVisitingStudent");
        Course introductory = new Course("ART101", "Introduction to Art", 3);
        CourseOffering visitingSection = new CourseOffering("20002", introductory, 2);
        VisitingStudent visiting = new VisitingStudent("V001", "Visitor");
        System.out.println("registered=" + service.register(visiting, visitingSection));
        System.out.println("before drop: enrollments="
                + visiting.getCurrentEnrollments().size()
                + ", seats used=" + visitingSection.getEnrolled());
        service.drop(visiting, visitingSection);
        System.out.println("after drop: enrollments="
                + visiting.getCurrentEnrollments().size()
                + ", seats used=" + visitingSection.getEnrolled());

        System.out.println("\nProvisionalStudent");
        CourseOffering provisionalSection = new CourseOffering("20003", introductory, 2);
        ProvisionalStudent provisional = new ProvisionalStudent("P001", "Provisional");
        System.out.println("registered=" + service.register(provisional, provisionalSection));
        System.out.println("current enrollments="
                + provisional.getCurrentEnrollments().size()
                + ", pending=" + provisional.getPending().size()
                + ", seats used=" + provisionalSection.getEnrolled());
    }
}