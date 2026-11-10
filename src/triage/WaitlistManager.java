package triage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TRIAGE EXERCISE
 *
 * Waitlist management for course offerings. In production since 2019.
 *
 * You have two days. Fix exactly one thing.
 * Read the context document before deciding.
 */
public class WaitlistManager {

    /** Department-level waitlist settings, loaded once at startup. */
    public static Map<String, String> settings = new HashMap<>();

    private final Map<String, List<WaitlistEntry>> byOffering = new HashMap<>();
    private final AuditLog audit;
    private final NotificationGateway notifications;

    public WaitlistManager(AuditLog audit, NotificationGateway notifications) {
        this.audit = audit;
        this.notifications = notifications;
    }

    /* ---------------------------------------------------------------- */

    public void addToWaitlist(String studentId, String crn, int priority) {
        List<WaitlistEntry> entries = byOffering.computeIfAbsent(crn, k -> new ArrayList<>());
        WaitlistEntry entry = new WaitlistEntry(studentId, crn, priority, Instant.now());
        entries.add(entry);
        entries.sort(Comparator.comparingInt(WaitlistEntry::getPriority)
                .thenComparing(WaitlistEntry::getAddedAt));
        audit.record("WAITLIST_ADD", studentId + " -> " + crn);
    }

    public void removeFromWaitlist(String studentId, String crn) {
        List<WaitlistEntry> entries = byOffering.get(crn);
        if (entries == null) return;
        entries.removeIf(e -> e.getStudentId().equals(studentId));
        audit.record("WAITLIST_REMOVE", studentId + " -> " + crn);
    }

    /**
     * Called when a seat opens. Promotes the next eligible student and
     * notifies them.
     */
    public String promoteNext(String crn) {
        List<WaitlistEntry> entries = byOffering.get(crn);
        if (entries == null || entries.isEmpty()) return null;

        int maxPromotions = Integer.parseInt(
                settings.getOrDefault("max_daily_promotions", "50"));

        if (promotedToday.size() >= maxPromotions) {
            audit.record("WAITLIST_THROTTLED", crn);
            return null;
        }

        WaitlistEntry next = entries.remove(0);
        promotedToday.add(next);

        String template = settings.getOrDefault("promotion_template",
                "A seat has opened in {course}. You have 48 hours to accept.");
        String body = template.replace("{course}", next.getCrn());
        notifications.send(next.getStudentId(), "Seat available", body);

        audit.record("WAITLIST_PROMOTE", next.getStudentId() + " -> " + crn);
        return next.getStudentId();
    }

    /** Cleared nightly by the batch job. */
    private final Set<WaitlistEntry> promotedToday = new HashSet<>();

    public void clearDailyPromotions() {
        promotedToday.clear();
    }

    /* ---------------------------------------------------------------- */

    public int positionOf(String studentId, String crn) {
        List<WaitlistEntry> entries = byOffering.get(crn);
        if (entries == null) return -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getStudentId().equals(studentId)) return i + 1;
        }
        return -1;
    }

    public int waitlistSize(String crn) {
        List<WaitlistEntry> entries = byOffering.get(crn);
        return entries == null ? 0 : entries.size();
    }

    public String formatWaitlistForAdvising(String crn) {
        List<WaitlistEntry> entries = byOffering.get(crn);
        if (entries == null || entries.isEmpty()) return "No students waitlisted.";

        StringBuilder out = new StringBuilder("Waitlist for " + crn + ":\n");
        for (int i = 0; i < entries.size(); i++) {
            WaitlistEntry e = entries.get(i);
            out.append("  ").append(i + 1).append(". ")
               .append(e.getStudentId())
               .append(" (priority ").append(e.getPriority()).append(")")
               .append("\n");
        }
        return out.toString();
    }

    public String exportCsv(String crn) {
        List<WaitlistEntry> entries = byOffering.get(crn);
        if (entries == null) return "";
        StringBuilder out = new StringBuilder("position,student_id,priority,added_at\n");
        for (int i = 0; i < entries.size(); i++) {
            WaitlistEntry e = entries.get(i);
            out.append(i + 1).append(",")
               .append(e.getStudentId()).append(",")
               .append(e.getPriority()).append(",")
               .append(e.getAddedAt()).append("\n");
        }
        return out.toString();
    }

    public void purgeExpiredOffers(String crn, Instant cutoff) {
        List<WaitlistEntry> entries = byOffering.get(crn);
        if (entries == null) return;
        entries.removeIf(e -> e.getAddedAt().isBefore(cutoff));
    }
}

/* -------------------------------------------------------------------- */

final class WaitlistEntry {

    private final String studentId;
    private final String crn;
    private final int priority;
    private final Instant addedAt;

    WaitlistEntry(String studentId, String crn, int priority, Instant addedAt) {
        this.studentId = studentId;
        this.crn = crn;
        this.priority = priority;
        this.addedAt = addedAt;
    }

    String getStudentId() { return studentId; }
    String getCrn() { return crn; }
    int getPriority() { return priority; }
    Instant getAddedAt() { return addedAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WaitlistEntry)) return false;
        WaitlistEntry that = (WaitlistEntry) other;
        return studentId.equals(that.studentId) && crn.equals(that.crn);
    }

    @Override
    public String toString() {
        return studentId + "@" + crn + "(p" + priority + ")";
    }
}
