public record RuleOutcome(boolean ok, String why) {
    public static final RuleOutcome PASS = new RuleOutcome(true, "");
}
