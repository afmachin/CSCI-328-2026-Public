import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 *
 *   cd demos/race
 *   javac RaceDemo.java && java RaceDemo
 *
 * The recorded output is in output.txt beside this file.
 *
 * The classes at the bottom are copied from baseline, with only the
 * `public` modifier dropped so this one file compiles on its own. Nothing about the
 * defect is invented for the demo: `CourseOffering` owns the seat count,
 * `Student.enroll` increments it, and the eligibility check reads `hasOpenSeats()`
 * somewhere else entirely. That is the shipped design.
 *
 * This program is nondeterministic by nature. Your numbers will not match the
 * recorded output in output.txt, and on a single-core machine or an
 * unlucky run some sections may report zero failures. Zero failures is not a
 * passing grade; it means the window was not hit this time.
 */
public class RaceDemo {

    static final int TRIALS = 20000;

    public static void main(String[] args) throws Exception {
        System.out.println("cores available to this JVM: "
                + Runtime.getRuntime().availableProcessors());
        System.out.println("trials per section: " + TRIALS);
        System.out.println();
        partOne();
        System.out.println();
        partTwo();
        System.out.println();
        partThree();
        System.out.println();
        partFour();
    }

    /** The bare mechanism: two owners of one number, and increments disappear. */
    static void partOne() throws Exception {
        System.out.println("== part one: the mechanism (lost updates) ==");
        System.out.println("Eight threads enrol one student each, 1000 times over, into a");
        System.out.println("section with room for everyone. The only correct answer is 8000.");

        CourseOffering offering = new CourseOffering("20001", new Course("CS201", 3), 100000);
        int threads = 8;
        int each = 1000;

        CountDownLatch start = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            Thread w = new Thread(() -> {
                await(start);
                for (int i = 0; i < each; i++) {
                    new Student("S" + i, "n", "UNDERGRAD").enroll(offering);
                }
            });
            w.start();
            workers.add(w);
        }
        start.countDown();
        for (Thread w : workers) w.join();

        int expected = threads * each;
        System.out.println("expected enrolled = " + expected);
        System.out.println("actual enrolled   = " + offering.getEnrolled());
        System.out.println("increments lost   = " + (expected - offering.getEnrolled()));
    }

    /** The course's own defect: check in one class, write in another. */
    static void partTwo() throws Exception {
        System.out.println("== part two: the last seat, as the codebase has it ==");
        System.out.println("capacity 20, 19 already enrolled, two students registering at once.");
        System.out.println("EligibilityChecker reads hasOpenSeats(); Student.enroll does the write.");

        int overfilled = 0;
        int worst = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            CourseOffering offering = lastSeat();
            race(offering, (student, o) -> {
                if (new EligibilityChecker().hasRoom(o)) {   // check
                    student.enroll(o);                        // act
                }
            });
            if (offering.getEnrolled() > 20) {
                overfilled++;
                worst = Math.max(worst, offering.getEnrolled());
            }
        }
        System.out.println("trials where enrolled exceeded capacity = "
                + overfilled + " of " + TRIALS);
        System.out.println("highest enrolled count observed         = " + Math.max(worst, 20));
    }

    /** Synchronizing the wrong method. The invariant still has two owners. */
    static void partThree() throws Exception {
        System.out.println("== part three: synchronized on the wrong thing ==");
        System.out.println("Student.enroll is now synchronized. The check is still outside it.");

        int overfilled = 0;
        int worst = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            CourseOffering offering = lastSeat();
            race(offering, (student, o) -> {
                if (new EligibilityChecker().hasRoom(o)) {
                    student.enrollSynchronized(o);
                }
            });
            if (offering.getEnrolled() > 20) {
                overfilled++;
                worst = Math.max(worst, offering.getEnrolled());
            }
        }
        System.out.println("trials where enrolled exceeded capacity = "
                + overfilled + " of " + TRIALS);
        System.out.println("highest enrolled count observed         = " + Math.max(worst, 20));
    }

    /** One owner. Check and write are the same operation, on the object that owns the count. */
    static void partFour() throws Exception {
        System.out.println("== part four: one owner ==");
        System.out.println("CourseOffering.claimSeat() checks and writes under its own lock,");
        System.out.println("and nothing outside CourseOffering writes the count at all.");

        int overfilled = 0;
        int refused = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            CourseOffering offering = lastSeat();
            Refusals r = new Refusals();
            race(offering, (student, o) -> {
                if (!o.claimSeat()) r.count++;
            });
            if (offering.getEnrolled() > 20) overfilled++;
            refused += r.count;
        }
        System.out.println("trials where enrolled exceeded capacity = "
                + overfilled + " of " + TRIALS);
        System.out.println("registrations refused for a full section = " + refused
                + "   (one per trial is the correct answer: " + TRIALS + ")");
    }

    // ---- harness ----

    interface Attempt { void run(Student student, CourseOffering offering); }

    static final class Refusals { int count; }

    static CourseOffering lastSeat() {
        CourseOffering offering = new CourseOffering("20001", new Course("CS201", 3), 20);
        for (int i = 0; i < 19; i++) offering.claimSeat();   // fill it the honest way
        return offering;
    }

    /** Two students attempt the same offering, released at the same instant. */
    static void race(CourseOffering offering, Attempt attempt) {
        CountDownLatch start = new CountDownLatch(1);
        Student a = new Student("S1", "Ada", "UNDERGRAD");
        Student b = new Student("S2", "Grace", "UNDERGRAD");
        Thread ta = new Thread(() -> { await(start); attempt.run(a, offering); });
        Thread tb = new Thread(() -> { await(start); attempt.run(b, offering); });
        ta.start();
        tb.start();
        start.countDown();
        try { ta.join(); tb.join(); } catch (InterruptedException e) { throw new RuntimeException(e); }
    }

    static void await(CountDownLatch latch) {
        try { latch.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
    }
}

/*
 * Below: copied from registration/, `public` dropped,
 * trimmed to the fields and methods this demo touches. Two additions are marked.
 */

class CourseOffering {

    private String crn;
    private Course course;
    private int capacity;
    private int enrolled;

    CourseOffering(String crn, Course course, int capacity) {
        this.crn = crn;
        this.course = course;
        this.capacity = capacity;
        this.enrolled = 0;
    }

    String getCrn() { return crn; }
    Course getCourse() { return course; }
    int getCapacity() { return capacity; }

    int getEnrolled() { return enrolled; }

    /*
     * The baseline's public setter. Parts one to three are built on it; part four does not
     * touch it. Note that it is still here: part four does not remove the setter, it stops
     * anything outside this class from needing one. Removing it is the design change the
     * demo is arguing for, and it is your job, not the demo's.
     */
    void setEnrolled(int enrolled) { this.enrolled = enrolled; }

    boolean hasOpenSeats() { return enrolled < capacity; }

    /** ADDED for part four: the check and the write, together, on the owner. */
    synchronized boolean claimSeat() {
        if (enrolled >= capacity) return false;
        enrolled = enrolled + 1;
        return true;
    }
}

class Student {

    private String id;
    private String name;
    private String level;
    private List<CourseOffering> currentEnrollments = new ArrayList<CourseOffering>();

    Student(String id, String name, String level) {
        this.id = id;
        this.name = name;
        this.level = level;
    }

    String getId() { return id; }
    String getName() { return name; }
    String getLevel() { return level; }

    void enroll(CourseOffering offering) {
        currentEnrollments.add(offering);
        offering.setEnrolled(offering.getEnrolled() + 1);
    }

    /** ADDED for part three: the same method, with a lock on the wrong object. */
    synchronized void enrollSynchronized(CourseOffering offering) {
        currentEnrollments.add(offering);
        offering.setEnrolled(offering.getEnrolled() + 1);
    }
}

class Course {
    private String code;
    private int credits;
    Course(String code, int credits) { this.code = code; this.credits = credits; }
    String getCode() { return code; }
    int getCredits() { return credits; }
}

/** Stands in for registration.EligibilityChecker: it reads the count it does not own. */
class EligibilityChecker {
    boolean hasRoom(CourseOffering offering) { return offering.hasOpenSeats(); }
}
