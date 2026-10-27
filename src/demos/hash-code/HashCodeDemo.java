import java.util.HashSet;
import java.util.Set;

/**
 * The equals/hashCode failure
 *
 *   cd reference/demos/hash-code
 *   javac HashCodeDemo.java && java HashCodeDemo
 *
 * The recorded output is in output.txt beside this file.
 *
 * Part two uses MeetingTime, copied verbatim from
 * registration/MeetingTime.java. It is your own codebase:
 * you can run the same experiment against the real class whenever you like.
 */
public class HashCodeDemo {

    public static void main(String[] args) {
        partOne();
        System.out.println();
        partTwo();
    }

    /** The bare mechanism: equal objects, unequal hash codes. */
    static void partOne() {
        System.out.println("== part one: the mechanism ==");

        Bad a = new Bad("CS201");
        Bad b = new Bad("CS201");   // equal to a, different hash code
        Bad c = new Bad("CS201");   // equal to both, never added

        Set<Bad> set = new HashSet<>();
        set.add(a);
        set.add(b);

        System.out.println("a.equals(b)     = " + a.equals(b));
        System.out.println("same hash code? = " + (a.hashCode() == b.hashCode()));
        System.out.println("set.size()      = " + set.size());
        System.out.println("set.contains(c) = " + set.contains(c));
    }

    /** The same failure in a class you have had since Week 1. */
    static void partTwo() {
        System.out.println("== part two: your own codebase ==");

        // Two sections that happen to meet at the same time.
        MeetingTime a = new MeetingTime("MON", 540, 630);
        MeetingTime b = new MeetingTime("MON", 540, 630);

        Set<MeetingTime> week = new HashSet<>();
        week.add(a);
        week.add(b);

        System.out.println("a.equals(b)                                   = " + a.equals(b));
        System.out.println("a.hashCode() == b.hashCode()                  = "
                + (a.hashCode() == b.hashCode()));
        System.out.println("distinct slots in the combined week           = "
                + week.size() + "   (advisor expects 1)");
        System.out.println("week.contains(new MeetingTime(\"MON\",540,630)) = "
                + week.contains(new MeetingTime("MON", 540, 630)));
    }

    /** Overrides equals, inherits Object.hashCode(), which is identity-based. */
    record Bad(String code) {
        @Override public boolean equals(Object o) {
            return o instanceof Bad other && other.code.equals(code);
        }
        @Override public int hashCode() { return System.identityHashCode(this); }
    }
}

/*
 * Copied from registration/MeetingTime.java.
 * Only the `public` modifier is dropped, so this one file compiles on its own;
 * every line below it is the class exactly as it ships.
 */
class MeetingTime {

    private String day;
    private int startMinutes;
    private int endMinutes;

    public MeetingTime(String day, int startMinutes, int endMinutes) {
        this.day = day;
        this.startMinutes = startMinutes;
        this.endMinutes = endMinutes;
    }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public int getStartMinutes() { return startMinutes; }
    public void setStartMinutes(int startMinutes) { this.startMinutes = startMinutes; }

    public int getEndMinutes() { return endMinutes; }
    public void setEndMinutes(int endMinutes) { this.endMinutes = endMinutes; }

    public boolean overlaps(MeetingTime other) {
        return day.equals(other.day)
                && startMinutes < other.endMinutes
                && other.startMinutes < endMinutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetingTime)) return false;
        MeetingTime that = (MeetingTime) o;
        return startMinutes == that.startMinutes
                && endMinutes == that.endMinutes
                && day.equals(that.day);
    }

    @Override
    public String toString() {
        return day + " " + startMinutes + "-" + endMinutes;
    }
}
