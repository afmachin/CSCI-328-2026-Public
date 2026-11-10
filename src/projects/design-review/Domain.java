package registration;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Shared domain types. Provided with the assignment; NOT under review.
 * All four submissions were written against exactly these types.
 */
final class Domain {
    private Domain() {}
}

enum Grade {
    A(4.0), B(3.0), C(2.0), D(1.0), F(0.0);

    private final double points;

    Grade(double points) { this.points = points; }

    double points() { return points; }

    /** Department policy: C or better counts as passing for prerequisite purposes. */
    boolean isPassing() { return points >= 2.0; }
}

enum StudentLevel { UNDERGRADUATE, GRADUATE }

record Course(String code, String title, int credits, Set<String> prerequisiteCodes) {}

record MeetingTime(DayOfWeek day, LocalTime start, LocalTime end) {
    boolean overlaps(MeetingTime other) {
        return day == other.day
            && start.isBefore(other.end)
            && other.start.isBefore(end);
    }
}

record CourseOffering(String crn, Course course, List<MeetingTime> meetings,
                      int capacity, int enrolledCount) {
    boolean hasOpenSeats() { return enrolledCount < capacity; }
}

record TranscriptEntry(String courseCode, Grade grade) {}

record Student(String id,
               String name,
               StudentLevel level,
               List<TranscriptEntry> transcript,
               List<CourseOffering> currentEnrollments) {}
