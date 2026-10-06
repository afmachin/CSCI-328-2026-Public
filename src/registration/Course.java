package registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Course {

    private final String code;
    private final String title;
    private final int credits;
    private final List<String> prerequisites;

    public Course(String code, String title, int credits) {
        this(code, title, credits, List.of());
    }

    public Course(String code, String title, int credits, List<String> prerequisites) {
        this.code = code;
        this.title = title;
        this.credits = credits;
        this.prerequisites = Collections.unmodifiableList(new ArrayList<>(prerequisites));
    }

    public String getCode() { return code; }
    public String getTitle() { return title; }
    public int getCredits() { return credits; }
    public List<String> getPrerequisites() { return prerequisites; }
}
