public class Main {
    public static void main(String[] args) {
        EligibilityRule graduate = ((EligibilityRule) Rules::prerequisites)
                .and(Rules::seatsAvailable)
                .and(Rules::creditLimit);

        try {
            graduate.evaluate(new EnrollmentRequest("S001", "10422"));
        } catch (RuntimeException e) {
            for (StackTraceElement f : e.getStackTrace()) System.out.println("at " + f);
        }
    }
}
