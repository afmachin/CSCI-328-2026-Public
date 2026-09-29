package registration;

import java.util.ArrayList;
import java.util.List;

public final class Student {

    private String id;
    private String name;
    private String level;
    private List<TranscriptEntry> transcript = new ArrayList<TranscriptEntry>();
    private List<CourseOffering> currentEnrollments = new ArrayList<CourseOffering>();

    public Student(String id, String name, String level) {
        this.id = id;
        this.name = name;
        this.level = level;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public List<TranscriptEntry> getTranscript() { return transcript; }
    public void setTranscript(List<TranscriptEntry> transcript) { this.transcript = transcript; }

    public List<CourseOffering> getCurrentEnrollments() { return currentEnrollments; }
    public void setCurrentEnrollments(List<CourseOffering> e) { this.currentEnrollments = e; }

    public void enroll(CourseOffering offering) {
        currentEnrollments.add(offering);
        offering.setEnrolled(offering.getEnrolled() + 1);
    }

    public int getCurrentCredits() {
        int total = 0;
        for (CourseOffering o : currentEnrollments) {
            total = total + o.getCourse().getCredits();
        }
        return total;
    }
}
