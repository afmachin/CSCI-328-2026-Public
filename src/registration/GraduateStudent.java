package registration;

public class GraduateStudent extends Student {

    private boolean advisorApproval;

    public GraduateStudent(String id, String name) {
        super(id, name, "GRADUATE");
    }

    public boolean hasAdvisorApproval() { return advisorApproval; }
    public void setAdvisorApproval(boolean advisorApproval) { this.advisorApproval = advisorApproval; }
}
