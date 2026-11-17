import java.util.ArrayList;
import java.util.List;

/**
 * Note 12a's "Working it through", as four runnable states of the same class.
 *
 * Step 0 pins the behaviour. Step 1 adds the seam. Step 2 extracts the rules and runs
 * into the friction the note predicts. Step 3 changes the behaviour and one pin breaks.
 *
 * Each step ships its own complete service class, so the four can be read side by side
 * and the diff between them is the lesson. The domain classes below are transcribed from
 * src/registration, trimmed to what these four exercise.
 */
public class RefactoringStepsDemo {

    /* ---------------------------------------------------------------- *
     *  The domain, transcribed from v0-baseline. Shared by every step.  *
     * ---------------------------------------------------------------- */

    /** Course.java: code, credits, and a mutable prerequisite list. */
    static final class Course {
        private final String code;
        private final int credits;
        private final List<String> prerequisites = new ArrayList<>();
        Course(String code, int credits) { this.code = code; this.credits = credits; }
        String getCode() { return code; }
        int getCredits() { return credits; }
        List<String> getPrerequisites() { return prerequisites; }
    }

    /** MeetingTime.java, overlaps() unchanged. */
    static final class MeetingTime {
        private final String day; private final int start, end;
        MeetingTime(String day, int start, int end) { this.day = day; this.start = start; this.end = end; }
        boolean overlaps(MeetingTime other) {
            return day.equals(other.day) && start < other.end && other.start < end;
        }
    }

    /** CourseOffering.java: capacity, a seat count the service increments, meetings. */
    static final class CourseOffering {
        private final String crn; private final Course course; private final int capacity;
        private int enrolled;
        private final List<MeetingTime> meetings = new ArrayList<>();
        CourseOffering(String crn, Course course, int capacity) {
            this.crn = crn; this.course = course; this.capacity = capacity;
        }
        String getCrn() { return crn; }
        Course getCourse() { return course; }
        int getEnrolled() { return enrolled; }
        void setEnrolled(int n) { this.enrolled = n; }
        List<MeetingTime> getMeetings() { return meetings; }
        boolean hasOpenSeats() { return enrolled < capacity; }
    }

    /** Student.java: enroll() mutates both sides, which is what Step 0 discovers. */
    static final class Student {
        private final String id; private final String name;
        private final List<CourseOffering> currentEnrollments = new ArrayList<>();
        Student(String id, String name) { this.id = id; this.name = name; }
        String getId() { return id; }
        String getName() { return name; }
        List<CourseOffering> getCurrentEnrollments() { return currentEnrollments; }
        void enroll(CourseOffering offering) {
            currentEnrollments.add(offering);
            offering.setEnrolled(offering.getEnrolled() + 1);
        }
        int getCurrentCredits() {
            int total = 0;
            for (CourseOffering o : currentEnrollments) total += o.getCourse().getCredits();
            return total;
        }
    }

    /** LegacySisClient.java: a singleton with a private constructor. */
    static final class LegacySisClient {
        private static LegacySisClient instance;
        private final List<String[]> rows = new ArrayList<>();
        private LegacySisClient() { }
        static LegacySisClient getInstance() {
            if (instance == null) instance = new LegacySisClient();
            return instance;
        }
        void seed(String studentId, String code, String grade) {
            rows.add(new String[] { studentId, code, grade });
        }
        String[][] fetchGradeRows(String studentId) {
            List<String[]> mine = new ArrayList<>();
            for (String[] r : rows) if (r[0].equals(studentId)) mine.add(new String[] { r[1], r[2] });
            return mine.toArray(new String[0][]);
        }
    }

    /** Config.java, reduced to the one key register() reads. */
    static final class Config {
        static int getInt(String key, int fallback) {
            return "max_credits".equals(key) ? 18 : fallback;
        }
    }

    private static boolean passing(String grade) { return !grade.equals("F") && !grade.equals("W"); }

    /* ---------------------------------------------------------------- *
     *  STEP 0 — the class as inherited. Early return, static reaches.   *
     * ---------------------------------------------------------------- */

    static final class Step0Service {
        private final List<String> lastErrors = new ArrayList<>();

        boolean register(Student student, CourseOffering offering) {
            lastErrors.clear();

            String[][] rows = LegacySisClient.getInstance().fetchGradeRows(student.getId());

            for (String[] row : rows) {
                if (row[0].equals(offering.getCourse().getCode()) && passing(row[1])) {
                    lastErrors.add("Already completed " + offering.getCourse().getCode());
                    return false;
                }
            }
            for (String prereq : offering.getCourse().getPrerequisites()) {
                boolean satisfied = false;
                for (String[] row : rows) if (row[0].equals(prereq) && passing(row[1])) satisfied = true;
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
                lastErrors.add("Section is full");
                return false;
            }
            student.enroll(offering);
            return true;
        }

        String describeResult(Student student, CourseOffering offering) {
            if (register(student, offering)) {
                return student.getName() + " registered for " + offering.getCourse().getCode();
            }
            StringBuilder out = new StringBuilder(student.getName() + " could not register:");
            for (String error : lastErrors) out.append("\n  - ").append(error);
            return out.toString();
        }
    }

    /* ---------------------------------------------------------------- *
     *  STEP 1 — the seam. Identical behaviour, one dependency named.    *
     * ---------------------------------------------------------------- */

    interface GradeSource {
        String[][] fetchGradeRows(String studentId);
    }

    /** The adapter you own, standing in front of the vendor's singleton. */
    static final class SisGradeSource implements GradeSource {
        public String[][] fetchGradeRows(String studentId) {
            return LegacySisClient.getInstance().fetchGradeRows(studentId);
        }
    }

    /** What the seam buys: a grade source with no SIS behind it. */
    static final class FakeGradeSource implements GradeSource {
        private final String[][] rows;
        FakeGradeSource(String[][] rows) { this.rows = rows; }
        public String[][] fetchGradeRows(String studentId) { return rows; }
    }

    static final class Step1Service {
        private final List<String> lastErrors = new ArrayList<>();
        private final GradeSource grades;

        Step1Service() { this(new SisGradeSource()); }          // every caller still compiles
        Step1Service(GradeSource grades) { this.grades = grades; }

        boolean register(Student student, CourseOffering offering) {
            lastErrors.clear();
            String[][] rows = grades.fetchGradeRows(student.getId());   // the only line that moved

            for (String[] row : rows) {
                if (row[0].equals(offering.getCourse().getCode()) && passing(row[1])) {
                    lastErrors.add("Already completed " + offering.getCourse().getCode());
                    return false;
                }
            }
            for (String prereq : offering.getCourse().getPrerequisites()) {
                boolean satisfied = false;
                for (String[] row : rows) if (row[0].equals(prereq) && passing(row[1])) satisfied = true;
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
                lastErrors.add("Section is full");
                return false;
            }
            student.enroll(offering);
            return true;
        }

        String describeResult(Student student, CourseOffering offering) {
            if (register(student, offering)) {
                return student.getName() + " registered for " + offering.getCourse().getCode();
            }
            StringBuilder out = new StringBuilder(student.getName() + " could not register:");
            for (String error : lastErrors) out.append("\n  - ").append(error);
            return out.toString();
        }
    }

    /* ---------------------------------------------------------------- *
     *  STEP 2 — rules extracted. Same behaviour, and the friction.      *
     * ---------------------------------------------------------------- *
     *
     *  Each rule needs to say two things: that it failed, and why. A
     *  boolean return carries one of them, so the error list goes in as
     *  a parameter. That parameter is the finding -- see the transcript.
     */

    static final class Step2Service {
        private final List<String> lastErrors = new ArrayList<>();
        private final GradeSource grades;

        Step2Service() { this(new SisGradeSource()); }
        Step2Service(GradeSource grades) { this.grades = grades; }

        boolean register(Student student, CourseOffering offering) {
            lastErrors.clear();
            String[][] rows = grades.fetchGradeRows(student.getId());

            if (!checkNotAlreadyPassed(rows, offering, lastErrors)) return false;
            if (!checkPrerequisites(rows, offering, lastErrors)) return false;
            if (!checkNoTimeConflict(student, offering, lastErrors)) return false;
            if (!checkCreditCap(student, offering, lastErrors)) return false;
            if (!checkOpenSeats(offering, lastErrors)) return false;

            student.enroll(offering);
            return true;
        }

        private boolean checkNotAlreadyPassed(String[][] rows, CourseOffering offering, List<String> errors) {
            for (String[] row : rows) {
                if (row[0].equals(offering.getCourse().getCode()) && passing(row[1])) {
                    errors.add("Already completed " + offering.getCourse().getCode());
                    return false;
                }
            }
            return true;
        }

        private boolean checkPrerequisites(String[][] rows, CourseOffering offering, List<String> errors) {
            for (String prereq : offering.getCourse().getPrerequisites()) {
                boolean satisfied = false;
                for (String[] row : rows) if (row[0].equals(prereq) && passing(row[1])) satisfied = true;
                if (!satisfied) {
                    errors.add("Missing prerequisite: " + prereq);
                    return false;
                }
            }
            return true;
        }

        private boolean checkNoTimeConflict(Student student, CourseOffering offering, List<String> errors) {
            for (CourseOffering current : student.getCurrentEnrollments()) {
                for (MeetingTime existing : current.getMeetings()) {
                    for (MeetingTime proposed : offering.getMeetings()) {
                        if (existing.overlaps(proposed)) {
                            errors.add("Time conflict with " + current.getCourse().getCode());
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        private boolean checkCreditCap(Student student, CourseOffering offering, List<String> errors) {
            int maxCredits = Config.getInt("max_credits", 18);
            int total = student.getCurrentCredits() + offering.getCourse().getCredits();
            if (total > maxCredits) {
                errors.add("Would total " + total + " credits, limit is " + maxCredits);
                return false;
            }
            return true;
        }

        private boolean checkOpenSeats(CourseOffering offering, List<String> errors) {
            if (!offering.hasOpenSeats()) {
                errors.add("Section is full");
                return false;
            }
            return true;
        }

        String describeResult(Student student, CourseOffering offering) {
            if (register(student, offering)) {
                return student.getName() + " registered for " + offering.getCourse().getCode();
            }
            StringBuilder out = new StringBuilder(student.getName() + " could not register:");
            for (String error : lastErrors) out.append("\n  - ").append(error);
            return out.toString();
        }
    }

    /* ---------------------------------------------------------------- *
     *  STEP 3 — the behaviour change. Every rule runs; all reasons kept.*
     * ---------------------------------------------------------------- */

    static final class Step3Service {
        private final List<String> lastErrors = new ArrayList<>();
        private final GradeSource grades;

        Step3Service() { this(new SisGradeSource()); }
        Step3Service(GradeSource grades) { this.grades = grades; }

        boolean register(Student student, CourseOffering offering) {
            lastErrors.clear();
            String[][] rows = grades.fetchGradeRows(student.getId());

            checkNotAlreadyPassed(rows, offering, lastErrors);
            checkPrerequisites(rows, offering, lastErrors);
            checkNoTimeConflict(student, offering, lastErrors);
            checkCreditCap(student, offering, lastErrors);
            checkOpenSeats(offering, lastErrors);

            if (!lastErrors.isEmpty()) return false;
            student.enroll(offering);
            return true;
        }

        private void checkNotAlreadyPassed(String[][] rows, CourseOffering offering, List<String> errors) {
            for (String[] row : rows) {
                if (row[0].equals(offering.getCourse().getCode()) && passing(row[1])) {
                    errors.add("Already completed " + offering.getCourse().getCode());
                }
            }
        }

        private void checkPrerequisites(String[][] rows, CourseOffering offering, List<String> errors) {
            for (String prereq : offering.getCourse().getPrerequisites()) {
                boolean satisfied = false;
                for (String[] row : rows) if (row[0].equals(prereq) && passing(row[1])) satisfied = true;
                if (!satisfied) errors.add("Missing prerequisite: " + prereq);
            }
        }

        private void checkNoTimeConflict(Student student, CourseOffering offering, List<String> errors) {
            for (CourseOffering current : student.getCurrentEnrollments()) {
                for (MeetingTime existing : current.getMeetings()) {
                    for (MeetingTime proposed : offering.getMeetings()) {
                        if (existing.overlaps(proposed)) {
                            errors.add("Time conflict with " + current.getCourse().getCode());
                            return;
                        }
                    }
                }
            }
        }

        private void checkCreditCap(Student student, CourseOffering offering, List<String> errors) {
            int maxCredits = Config.getInt("max_credits", 18);
            int total = student.getCurrentCredits() + offering.getCourse().getCredits();
            if (total > maxCredits) {
                errors.add("Would total " + total + " credits, limit is " + maxCredits);
            }
        }

        private void checkOpenSeats(CourseOffering offering, List<String> errors) {
            if (!offering.hasOpenSeats()) errors.add("Section is full");
        }

        String describeResult(Student student, CourseOffering offering) {
            if (register(student, offering)) {
                return student.getName() + " registered for " + offering.getCourse().getCode();
            }
            StringBuilder out = new StringBuilder(student.getName() + " could not register:");
            for (String error : lastErrors) out.append("\n  - ").append(error);
            return out.toString();
        }
    }

    /* ---------------------------------------------------------------- *
     *  The pin harness, and the five cases Step 0 of the note asks for. *
     * ---------------------------------------------------------------- */

    /** A pin: a named case, and what it printed on the day it was recorded. */
    record Pin(String name, String recorded) { }

    /** The five situations. Each call builds them fresh, so runs cannot interfere. */
    interface Case { String run(); }

    private static List<Pin> record(java.util.function.Function<String, String> run) {
        List<Pin> pins = new ArrayList<>();
        for (String name : CASE_NAMES) pins.add(new Pin(name, run.apply(name)));
        return pins;
    }

    private static final List<String> CASE_NAMES = List.of(
            "clean enrolment", "missing prerequisite", "over the credit cap",
            "section full", "two failures at once");

    private static int check(List<Pin> pins, java.util.function.Function<String, String> run) {
        int broken = 0;
        for (Pin pin : pins) {
            String now = run.apply(pin.name());
            if (now.equals(pin.recorded())) {
                System.out.println("      pass   " + pin.name());
            } else {
                broken++;
                System.out.println("      BREAK  " + pin.name());
                System.out.println("             recorded: " + flat(pin.recorded()));
                System.out.println("             now:      " + flat(now));
            }
        }
        return broken;
    }

    private static String flat(String s) { return s.replace("\n  - ", " / ").replace("\n", " "); }

    /** Builds one of the five cases and runs it through whichever service is given. */
    private static String runCase(String name, Object service) {
        Course cs101 = new Course("CS101", 4);
        Course cs201 = new Course("CS201", 4);
        cs201.getPrerequisites().add("CS101");

        String id = switch (name) {
            case "missing prerequisite" -> "S002";          // CS101 graded F
            case "two failures at once" -> "S009";          // CS101 graded F, and loaded up below
            default -> "S001";                              // CS101 graded B
        };
        String who = switch (name) {
            case "missing prerequisite" -> "Ben Okafor";
            case "two failures at once" -> "Dana Vale";
            default -> "Alice Reyes";
        };
        Student student = new Student(id, who);
        int capacity = name.equals("section full") ? 1 : 5;
        CourseOffering target = new CourseOffering("10422", cs201, capacity);

        if (name.equals("over the credit cap") || name.equals("two failures at once")) {
            for (int i = 0; i < 4; i++) {
                student.enroll(new CourseOffering("F" + i, new Course("FIL" + i, 4), 9));
            }
        }
        if (name.equals("section full")) target.setEnrolled(1);

        if (service instanceof Step0Service s) return s.describeResult(student, target);
        if (service instanceof Step1Service s) return s.describeResult(student, target);
        if (service instanceof Step2Service s) return s.describeResult(student, target);
        if (service instanceof Step3Service s) return s.describeResult(student, target);
        throw new IllegalStateException();
    }

    /* ---------------------------------------------------------------- *
     *  main                                                            *
     * ---------------------------------------------------------------- */

    public static void main(String[] args) {
        LegacySisClient sis = LegacySisClient.getInstance();
        sis.seed("S001", "CS101", "B");      // Alice passed the prerequisite
        sis.seed("S002", "CS101", "F");      // Ben did not
        sis.seed("S009", "CS101", "F");      // nor did Dana, who is also over the cap

        List<Pin> pins = step0();
        step1(pins);
        step2(pins);
        step3(pins);
    }

    /** Step 0 — pin five cases, and find a defect by running the code twice. */
    private static List<Pin> step0() {
        System.out.println("=== Step 0 - pin ===");
        System.out.println();
        System.out.println("  Recording what the inherited class does. Not what it should do.");
        System.out.println();
        List<Pin> pins = record(name -> runCase(name, new Step0Service()));
        for (Pin pin : pins) System.out.println("      " + pad(pin.name()) + flat(pin.recorded()));

        System.out.println();
        System.out.println("  Now run the same query twice, to check the pin is stable.");
        System.out.println("  The offering has no MeetingTime on it -- that matters, see below.");
        System.out.println();
        Course cs201 = new Course("CS201", 4);
        cs201.getPrerequisites().add("CS101");
        CourseOffering ds = new CourseOffering("10422", cs201, 2);
        Student alice = new Student("S001", "Alice Reyes");
        Step0Service service = new Step0Service();

        System.out.printf("      enrollments before:     %d, seats taken: %d%n",
                alice.getCurrentEnrollments().size(), ds.getEnrolled());
        service.describeResult(alice, ds);
        System.out.printf("      after 1 describeResult: %d, seats taken: %d%n",
                alice.getCurrentEnrollments().size(), ds.getEnrolled());
        service.describeResult(alice, ds);
        System.out.printf("      after 2 describeResult: %d, seats taken: %d%n",
                alice.getCurrentEnrollments().size(), ds.getEnrolled());
        System.out.printf("      capacity is %d; open seats? %b%n", 2, ds.hasOpenSeats());
        System.out.println();
        System.out.println("  One student, asked about twice, has consumed a two-seat section.");
        System.out.println("  The act of pinning found a defect that reading did not.");
        System.out.println();
        System.out.println("  Put meetings on that offering and you see something else instead:");
        CourseOffering withMeetings = new CourseOffering("10422", cs201, 2);
        withMeetings.getMeetings().add(new MeetingTime("MON", 540, 630));
        Student bea = new Student("S001", "Alice Reyes");
        Step0Service s2 = new Step0Service();
        s2.describeResult(bea, withMeetings);
        System.out.println("      second call: " + flat(s2.describeResult(bea, withMeetings)));
        System.out.printf("      seats taken: %d -- the time-conflict rule caught her own enrolment.%n",
                withMeetings.getEnrolled());
        System.out.println("      Same defect underneath; a longer story to tell. Pin without meetings.");
        System.out.println();
        return pins;
    }

    /** Step 1 — add the seam. Nothing observable may change. */
    private static void step1(List<Pin> pins) {
        System.out.println("=== Step 1 - seam ===");
        System.out.println();
        System.out.println("  GradeSource, SisGradeSource, and a no-argument constructor that");
        System.out.println("  delegates. The class had no explicit constructor before, so writing");
        System.out.println("  that one back by hand is what keeps every caller compiling.");
        System.out.println();
        int broken = check(pins, name -> runCase(name, new Step1Service()));
        System.out.println();
        System.out.println("  " + broken + " pins broken. Commit.");
        System.out.println();
        System.out.println("  And here is what the seam bought, which Step 0 could not do at all:");
        Course cs201 = new Course("CS201", 4);
        cs201.getPrerequisites().add("CS101");
        Student nobody = new Student("S404", "Unknown Student");
        Step1Service faked = new Step1Service(
                new FakeGradeSource(new String[][] { { "CS101", "A" } }));
        System.out.println("      a student the SIS has never heard of, with a fake grade source:");
        System.out.println("      " + flat(faked.describeResult(nobody, new CourseOffering("1", cs201, 5))));
        System.out.println();
    }

    /** Step 2 — extract the rules. Same behaviour; the friction is the point. */
    private static void step2(List<Pin> pins) {
        System.out.println("=== Step 2 - extract, one rule at a time ===");
        System.out.println();
        System.out.println("  Five private methods, one per rule. Behaviour unchanged:");
        System.out.println();
        int broken = check(pins, name -> runCase(name, new Step2Service()));
        System.out.println();
        System.out.println("  " + broken + " pins broken -- but look at what the extraction cost.");
        System.out.println();
        System.out.println("      private boolean checkPrerequisites(String[][] rows,");
        System.out.println("                                         CourseOffering offering,");
        System.out.println("                                         List<String> errors)");
        System.out.println();
        System.out.println("  Each rule has to report two things: that it failed, and why. A boolean");
        System.out.println("  carries the first. The second had to be passed in as a mutable list for");
        System.out.println("  the method to write into -- so every extracted rule now takes an");
        System.out.println("  out-parameter it only ever appends to.");
        System.out.println();
        System.out.println("  That parameter is not a wart in the refactoring. It is the design");
        System.out.println("  telling you that a rule's result is a value nobody modelled, and it is");
        System.out.println("  the same reason the caller cannot report more than one failure.");
        System.out.println();
    }

    /** Step 3 — change the behaviour. Exactly one pin may break. */
    private static void step3(List<Pin> pins) {
        System.out.println("=== Step 3 - now change the behaviour ===");
        System.out.println();
        System.out.println("  Every rule runs; every reason is kept. This is the feature, and it is");
        System.out.println("  a separate commit from Steps 1 and 2.");
        System.out.println();
        int broken = check(pins, name -> runCase(name, new Step3Service()));
        System.out.println();
        System.out.println("  " + broken + " pin broken, and it is the right one.");
        System.out.println();
        System.out.println("  Four cases did not move, because a case that fails one rule reports one");
        System.out.println("  rule whether you return early or collect. Four pins passing was never");
        System.out.println("  evidence that the change worked -- it was evidence that four of the five");
        System.out.println("  could not tell. Only the case built to fail twice can see this change,");
        System.out.println("  which is why Step 0 recorded it.");
    }

    private static String pad(String name) {
        StringBuilder b = new StringBuilder(name);
        while (b.length() < 24) b.append(' ');
        return b.toString();
    }
}
