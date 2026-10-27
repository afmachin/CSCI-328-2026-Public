package registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SUBMISSION B
 *
 * Developer note included with the submission:
 * "I used the Factory, Singleton, Builder, Strategy and Visitor patterns so
 *  that new eligibility rules can be added later without modifying existing
 *  code, following the Open/Closed Principle."
 */
public class EligibilityEngine {

    private static EligibilityEngine INSTANCE;

    private final List<AbstractEligibilityRule> rules = new ArrayList<>();

    private EligibilityEngine() {
        RuleFactoryProvider provider = RuleFactoryProvider.getInstance();
        for (String ruleName : provider.getConfiguredRuleNames()) {
            rules.add(provider.getFactory(ruleName).createRule());
        }
    }

    public static synchronized EligibilityEngine getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EligibilityEngine();
        }
        return INSTANCE;
    }

    public EligibilityReport evaluate(Student student, CourseOffering offering) {
        EligibilityReport.Builder builder = new EligibilityReport.Builder();
        EvaluatingRuleVisitor visitor = new EvaluatingRuleVisitor(student, offering, builder);
        for (AbstractEligibilityRule rule : rules) {
            rule.accept(visitor);
        }
        return builder.build();
    }
}

/* ------------------------------------------------------------------ */
/* Rule hierarchy                                                      */
/* ------------------------------------------------------------------ */

interface RuleVisitor {
    void visitPrerequisiteRule(PrerequisiteRule rule);
    void visitCapacityRule(CapacityRule rule);
    void visitScheduleRule(ScheduleRule rule);
    void visitCreditLimitRule(CreditLimitRule rule);
    void visitDuplicateEnrollmentRule(DuplicateEnrollmentRule rule);
}

abstract class AbstractEligibilityRule {

    private RuleMessageStrategy messageStrategy = new DefaultRuleMessageStrategy();

    public abstract void accept(RuleVisitor visitor);

    public abstract String getRuleName();

    public RuleMessageStrategy getMessageStrategy() { return messageStrategy; }

    public void setMessageStrategy(RuleMessageStrategy strategy) {
        this.messageStrategy = strategy;
    }
}

abstract class AbstractStudentRule extends AbstractEligibilityRule {
    protected boolean appliesToLevel(StudentLevel level) { return true; }
}

abstract class AbstractOfferingRule extends AbstractEligibilityRule {
    protected boolean requiresOffering() { return true; }
}

class PrerequisiteRule extends AbstractStudentRule {
    public void accept(RuleVisitor v) { v.visitPrerequisiteRule(this); }
    public String getRuleName() { return "PREREQUISITE"; }
}

class CreditLimitRule extends AbstractStudentRule {
    public void accept(RuleVisitor v) { v.visitCreditLimitRule(this); }
    public String getRuleName() { return "CREDIT_LIMIT"; }
}

class DuplicateEnrollmentRule extends AbstractStudentRule {
    public void accept(RuleVisitor v) { v.visitDuplicateEnrollmentRule(this); }
    public String getRuleName() { return "DUPLICATE"; }
}

class CapacityRule extends AbstractOfferingRule {
    public void accept(RuleVisitor v) { v.visitCapacityRule(this); }
    public String getRuleName() { return "CAPACITY"; }
}

class ScheduleRule extends AbstractOfferingRule {
    public void accept(RuleVisitor v) { v.visitScheduleRule(this); }
    public String getRuleName() { return "SCHEDULE"; }
}

/* ------------------------------------------------------------------ */
/* Message strategy                                                    */
/* ------------------------------------------------------------------ */

interface RuleMessageStrategy {
    String format(String ruleName, String detail);
}

class DefaultRuleMessageStrategy implements RuleMessageStrategy {
    public String format(String ruleName, String detail) {
        return "[" + ruleName + "] " + detail;
    }
}

/* ------------------------------------------------------------------ */
/* Factories                                                           */
/* ------------------------------------------------------------------ */

interface EligibilityRuleFactory {
    AbstractEligibilityRule createRule();
}

class RuleFactoryProvider {

    private static final RuleFactoryProvider INSTANCE = new RuleFactoryProvider();

    private final Map<String, EligibilityRuleFactory> factories = new LinkedHashMap<>();

    private RuleFactoryProvider() {
        factories.put("PREREQUISITE", PrerequisiteRule::new);
        factories.put("CAPACITY", CapacityRule::new);
        factories.put("SCHEDULE", ScheduleRule::new);
        factories.put("CREDIT_LIMIT", CreditLimitRule::new);
        factories.put("DUPLICATE", DuplicateEnrollmentRule::new);
    }

    public static RuleFactoryProvider getInstance() { return INSTANCE; }

    public List<String> getConfiguredRuleNames() {
        return new ArrayList<>(factories.keySet());
    }

    public EligibilityRuleFactory getFactory(String name) {
        return factories.get(name);
    }
}

/* ------------------------------------------------------------------ */
/* Report + builder                                                    */
/* ------------------------------------------------------------------ */

class EligibilityReport {

    private final boolean eligible;
    private final List<String> messages;

    private EligibilityReport(boolean eligible, List<String> messages) {
        this.eligible = eligible;
        this.messages = messages;
    }

    public boolean isEligible() { return eligible; }

    public List<String> getMessages() { return Collections.unmodifiableList(messages); }

    static class Builder {
        private final List<String> messages = new ArrayList<>();

        Builder addMessage(String message) {
            messages.add(message);
            return this;
        }

        EligibilityReport build() {
            return new EligibilityReport(messages.isEmpty(), messages);
        }
    }
}

/* ------------------------------------------------------------------ */
/* The visitor that actually does the work                             */
/* ------------------------------------------------------------------ */

class EvaluatingRuleVisitor implements RuleVisitor {

    private static final int MAX_CREDITS = 18;

    private final Student student;
    private final CourseOffering offering;
    private final EligibilityReport.Builder builder;

    EvaluatingRuleVisitor(Student student, CourseOffering offering,
                          EligibilityReport.Builder builder) {
        this.student = student;
        this.offering = offering;
        this.builder = builder;
    }

    public void visitPrerequisiteRule(PrerequisiteRule rule) {
        for (String prereq : offering.course().prerequisiteCodes()) {
            boolean satisfied = false;
            for (TranscriptEntry entry : student.transcript()) {
                if (entry.courseCode().equals(prereq) && entry.grade().isPassing()) {
                    satisfied = true;
                }
            }
            if (!satisfied) {
                builder.addMessage(rule.getMessageStrategy()
                        .format(rule.getRuleName(), "Missing prerequisite: " + prereq));
            }
        }
    }

    public void visitCapacityRule(CapacityRule rule) {
        if (!offering.hasOpenSeats()) {
            builder.addMessage(rule.getMessageStrategy()
                    .format(rule.getRuleName(), "Section " + offering.crn() + " is full"));
        }
    }

    public void visitScheduleRule(ScheduleRule rule) {
        for (CourseOffering current : student.currentEnrollments()) {
            for (MeetingTime existing : current.meetings()) {
                for (MeetingTime proposed : offering.meetings()) {
                    if (existing.overlaps(proposed)) {
                        builder.addMessage(rule.getMessageStrategy().format(
                                rule.getRuleName(),
                                "Conflicts with " + current.course().code()));
                    }
                }
            }
        }
    }

    public void visitCreditLimitRule(CreditLimitRule rule) {
        int total = offering.course().credits();
        for (CourseOffering current : student.currentEnrollments()) {
            total += current.course().credits();
        }
        if (total > MAX_CREDITS) {
            builder.addMessage(rule.getMessageStrategy()
                    .format(rule.getRuleName(), "Total would be " + total + " credits"));
        }
    }

    public void visitDuplicateEnrollmentRule(DuplicateEnrollmentRule rule) {
        for (TranscriptEntry entry : student.transcript()) {
            if (entry.courseCode().equals(offering.course().code())
                    && entry.grade().isPassing()) {
                builder.addMessage(rule.getMessageStrategy()
                        .format(rule.getRuleName(), "Already completed with grade "
                                + entry.grade()));
            }
        }
    }
}
