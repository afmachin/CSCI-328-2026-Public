# Project Design Review

**Branch:** `ProjectDesignReview`  
**Package of supplied code:** `registration` (files stored in `src/projects/design-review/`)  
**Reviewed submissions:** A: `RegistrationService.java`; B: `EligibilityEngine.java`; C: `EnrollmentChecker.java`; D: `EligibilityRules.java`  
**Shared file:** `Domain.java` — unchanged

## Review standard

The original specification requires five checks: prerequisite grades of C or better, an open seat, no schedule conflicts, no previously completed passing grade for the requested course, and a total of at most 18 credits. The caller must receive **every reason** for ineligibility, not just the first. Eligibility checking should not secretly enroll a student. I evaluated each design against those requirements and three change requests. For CR-1, undergraduate students still have the 18-credit limit; graduate students have a 12-credit limit **unless they have written advisor approval**, and they are exempt from the undergraduate 18-credit cap. CR-2 requires a student-specific, auditable waiver of a rule that remains visible as *waived*. CR-3 requires comparing normal eligibility with a hypothetical course drop **without changing real enrollments**.

Severity meanings used here: **Structural** = responsibilities or the model must change; **Contract** = a language/library or exposed-method contract is broken; **Correctness** = the requested behavior is wrong; **Cosmetic** = a smaller presentation issue. I focused on findings that have real consequences rather than listing style complaints.

---

## Submission A — `RegistrationService.java`

### Findings

**A1 — Correctness: stops after the first failure (lines 31, 45, 52, 61, 74).** Every failed check immediately returns `false`. If a student lacks a prerequisite, has a schedule conflict, and would exceed the credit limit, the advising screen may only say that a prerequisite is missing. The student can fix that problem and discover the other blockers only after checking again. This directly violates the requirement to report every reason.

**A2 — Contract / Correctness: a query changes the student's enrollments (lines 77 and 82).** `canEnroll` appends the offering to `student.currentEnrollments()` when all checks pass. `describeResult` calls that same method simply to display a message, so opening the advising screen can enroll the student. With an unmodifiable enrollment list, it can throw an `UnsupportedOperationException` instead. Repeated checks can also add the same offering more than once.

**A3 — Structural: the review result is stored as mutable service state (lines 17–20, 23–24).** `lastErrors` belongs to the service instead of the individual check result, and `getLastErrors()` returns the actual internal list. Another caller can clear or edit the errors, while a later request clears the earlier result. Reusing one service across students can therefore display the wrong reasons or a changed list. The method should return a self-contained, read-only decision.

**A4 — Correctness: the enrollment side effect does not update available-seat information (line 77).** The method changes a student's enrollment list but leaves the immutable `CourseOffering.enrolledCount()` as it was. A following check can still see the old open-seat count and allow additional enrollments that should be blocked. Actual registration and capacity updates need a separate, consistent operation; this eligibility checker cannot safely do both.

### Primary defect

**A2: `canEnroll` mutates enrollments.** I would raise this first because even a successful *read-only* advising request changes real data or throws an exception. The early returns are also serious, but incorrect reporting is less dangerous than an eligibility screen silently registering someone.

### Change-request cost

| Request | Assessment | Required changes |
|---|---|---|
| **CR-1** | **Accommodates with effort** | Rewrite the credit check in `RegistrationService.canEnroll` to branch on `Student.level()` and obtain an external written-approval record. `Domain.Student` cannot be changed. Graduate students at 13+ credits need approval; graduates with approval are not restricted by the undergraduate 18-credit cap. |
| **CR-2** | **Requires the design to be reworked** | Replace `lastErrors` strings and early returns with typed, per-rule outcomes. Add a separate waiver record/service keyed by student and rule, with grantor and time; preserve a *waived* outcome in the returned decision. |
| **CR-3** | **Requires the design to be reworked** | Separate evaluation from enrollment mutation, pass a hypothetical enrollment list or request context, evaluate before and after a dropped course, and compare typed reasons. Calling `describeResult` twice is unsafe because it may enroll the student. |

### Refactoring sketch

First I would stop treating `canEnroll` as both a check and a registration command. Its only job should be to inspect the supplied information and return a decision. A separate registration method would take responsibility for actually changing enrollments and for updating seat counts consistently. I would not alter `Domain.java`, because the provided records are shared types and the assignment specifically says not to modify them. I would introduce an `EnrollmentRequest` or similar context containing the student, requested offering, and an optional hypothetical dropped offering. The decision would hold a read-only list of typed results, including which rule failed and the useful details. Then I would change each rule check to add its result without returning early. That fixes the main original specification problem before working on the new requirements. Next I would add written graduate-approval lookup to the credit policy and a separate record of who approved a rule waiver and when. A waiver should change a failed result to *waived* without erasing it from the advising screen. Finally I would make the what-if view evaluate filtered enrollments without touching the student's real list. I would deliberately leave the existing `Grade.isPassing()`, `MeetingTime.overlaps()`, and `CourseOffering.hasOpenSeats()` behavior alone. Those shared helpers already express the relevant domain rules. I would also avoid adding a large hierarchy of subclasses just to solve what is currently a small set of checks.

---

## Submission B — `EligibilityEngine.java`

### Findings

**B1 — Structural: the Visitor owns the actual rules (lines 51–57 and 190–261).** The five rule classes mostly contain names and `accept` methods; `EvaluatingRuleVisitor` contains the eligibility decisions. Adding a new rule requires changing the visitor interface, the visitor implementation, the rule hierarchy, and the factory list. The promised ability to add rules without editing existing code is not real. A registrar policy change would require edits across several unrelated types, increasing the risk of missing a place.

**B2 — Structural: reports contain only formatted strings, not rule outcomes (lines 159–181 and 205–259).** Although strings include a prefix such as `[SCHEDULE]`, the report has no stable typed reason, student-specific waiver state, or success/waived status. An advising screen implementing CR-2 would have to parse display text or redesign the report; changing wording could then break behavior.

**B3 — Structural: the shared visitor binds evaluation to real student enrollments (lines 196–202 and 227–242).** Schedule and credit calculations read `student.currentEnrollments()` directly. There is no request context for a hypothetical dropped course. For CR-3, copying or filtering the enrollment state must be introduced outside or throughout this shared evaluator, making it easy for one rule to use the real list while another uses the hypothetical list.

**B4 — Cosmetic: schedule conflicts may be repeated for the same course (lines 227–235).** The visitor adds a message for every overlapping meeting pair instead of one per meaningful conflict. If two meetings in the same enrolled course both overlap, the advising list can show repeated messages that make it harder to see distinct problems. This is much cheaper to fix than the structural issues.

### Primary defect

**B1: the Visitor centralizes all policy logic.** It is the most important long-term defect because it undermines the main benefit the design claims to provide. Each policy change has a large edit footprint even though there are many rule classes.

### Change-request cost

| Request | Assessment | Required changes |
|---|---|---|
| **CR-1** | **Accommodates with effort** | Edit `EvaluatingRuleVisitor.visitCreditLimitRule` to use `Student.level()` and obtain written advisor approval via an injected dependency. The `AbstractStudentRule.appliesToLevel()` hook does not help because the visitor never calls it. |
| **CR-2** | **Requires the design to be reworked** | Replace the string-only `EligibilityReport`/`Builder` with typed outcomes and waiver metadata. Provide student-specific waiver lookup to the visitor and preserve waived reasons alongside failures. Existing `RuleMessageStrategy` changes display text, not the decision state. |
| **CR-3** | **Accommodates with effort** | Give `EligibilityEngine.evaluate` a request context or filtered copy of the student and have the visitor use a proposed enrollment list for both time and credit checks. Compare normal and hypothetical reports by stable rule identities, not just raw strings. |

### Refactoring sketch

I would start by moving the real check behavior out of `EvaluatingRuleVisitor` and into the respective rule implementations. The current rule classes say which rule they are, but the visitor is where all the decisions happen. That creates a second place to edit every time the registrar adds or changes a policy. A small rule interface such as `evaluate(request)` returning typed outcomes would be enough for this exercise. `EligibilityEngine` could simply iterate over a list of those rules and aggregate all the results. I would remove the visitor methods and the factory/provider setup unless actual runtime configuration is needed. This keeps new checks local instead of touching every visitor declaration. Then I would return a read-only structured report with rule IDs, relevant data, and explicit failed, passed, or waived status. A graduate-approval dependency and a waiver lookup could be part of the evaluation context, while audit records should be owned outside the checker. For the what-if feature, I would let the context expose an effective enrollment list that leaves out the course the student is considering dropping. Both schedule and credit checks would use the same effective list. I would deliberately preserve the useful part of this submission: it currently evaluates the rules without enrolling anyone and generally gathers more than one failure. I would also keep the existing grade, overlap, and seat checks rather than rewriting those correct calculations. The goal is fewer moving parts and more reliable change points, not simply fewer classes.

---

## Submission C — `EnrollmentChecker.java`

### Findings

**C1 — Contract: `Violation` overrides `equals` but not `hashCode` (lines 63–70, 87–89).** Equal violations can have different object hash codes, breaking Java's equality/hash contract. `distinctViolations()` puts the objects in a `HashSet`, so equivalent violations can appear more than once if duplicate violations are produced. The code will behave unpredictably when these values are used in hashed collections.

**C2 — Contract / Correctness: a read-only schedule check sorts the actual enrollment list (line 140).** `ScheduleCheck` changes the order of `student.currentEnrollments()` while evaluating eligibility. If the list is immutable (for example, created with `List.of`), `sort` throws `UnsupportedOperationException` and no complete decision is returned. With a mutable list, other code sees the student's courses reordered by a query.

**C3 — Structural: one string per rule cannot represent all individual problems or waiver states (lines 42–47 and 145–147).** `RuleCheck.violationOrNull()` returns only one message. The schedule check returns after the first conflicting course, so a second simultaneous conflict is hidden. The combined prerequisite string includes all missing course codes, which is useful, but individual prerequisites still are not separate typed outcomes that CR-2 could distinguish or mark waived. The screen could wrongly suggest that dropping the first conflicting course clears every conflict.

**C4 — Correctness: callers can change a completed decision (lines 79–85).** `EnrollmentDecision` stores and returns the same mutable list. Calling `getViolations().clear()` after a failed check makes `isAllowed()` return `true` without re-evaluating any rule. This allows accidental or intentional modification of the eligibility result after the checker returns it.

### Primary defect

**C2: sorting the student's real enrollments inside a check.** I would raise this first because a normal student record with an unmodifiable list can crash every eligibility check that reaches the schedule rule, and mutable records are silently altered. The missing `hashCode` is also a definite Java contract bug, but the sorting problem can stop the advising workflow altogether.

### Change-request cost

| Request | Assessment | Required changes |
|---|---|---|
| **CR-1** | **Accommodates with effort** | Change `CreditLimitCheck` to use `Student.level()` plus a written advisor-approval lookup. Since the rule receives only `Student` and `CourseOffering`, approval needs an injected service or a richer evaluation context. |
| **CR-2** | **Requires the design to be reworked** | Replace `String violationOrNull` with typed, possibly multiple, rule outcomes; add student/rule-specific waivers and grantor/timestamp data, and update `EnrollmentDecision` to preserve waived entries on screen. |
| **CR-3** | **Accommodates with effort** | Evaluate a copied/filtered enrollment list for a proposed drop, compare stable violations, and remove the sorting side effect. The existing `Student` record can be used in a new request context without changing `Domain.java`. |

### Refactoring sketch

My first change would be to stop sorting the student's own enrollment list. The schedule check can iterate in the existing order, or sort a separate copy if deterministic output is important. I would then add a matching `hashCode()` to `Violation`, or more simply make it a record, so the equals/hash contract is correct. Those are small fixes with an immediate effect on safety. Next I would make `EnrollmentDecision` defensive: copy the incoming violations and return an unmodifiable list. That way a UI cannot change the answer after a check. The larger redesign would be changing `RuleCheck` from returning one nullable string to returning a list of typed results with a rule ID and details. This allows all schedule conflicts and missing prerequisites to be shown separately, while still collecting problems from every rule. A waiver record would include student ID, rule ID, grantor, and time, and evaluation would produce *waived* outcomes instead of deleting failed ones. For CR-1 the credit check would use student level and an approval service, not hard-code the same 18-credit cap for everyone. For CR-3 I would pass the same filtered enrollment view to both credit and schedule checks and compare baseline and what-if outcomes. I would leave the useful rule separation and the existing no-early-exit loop in `EnrollmentChecker` alone. The individual checks are already reasonably local; the problem is their result contract and the hidden state change, not the number of classes.

---

## Submission D — `EligibilityRules.java`

### Findings

**D1 — Correctness: only the first missing prerequisite is reported (lines 41–48).** `prerequisitesMet` returns immediately after the first missing prerequisite. If a course requires two classes and both are missing, the advising screen sees only one reason. The code checks every **rule category**, but not every reason inside that category, so it misses part of the original specification.

**D2 — Correctness: only the first conflicting enrolled course is reported (lines 59–70).** `noScheduleConflict` returns on the first overlap. If two enrolled courses conflict with the proposed course, the second is hidden. A what-if screen might claim dropping the first course removes the schedule issue, only to uncover the second conflict later.

**D3 — Structural: the rule interface is limited to one outcome per check (lines 106–112 and 128–135).** `EligibilityRule.evaluate` returns one `RuleOutcome`, and the aggregator adds exactly one outcome from each rule. Even after removing the early returns inside prerequisite and schedule checks, there is nowhere to put multiple independently identifiable problems within a rule without changing the result shape. This is the main change needed to make “all reasons” reliable.

### Primary defect

**D3: the one-outcome-per-rule model.** It is the source of both missing-result problems. The typed reasons, read-only decision list, and separation of message rendering are good foundations, but a caller cannot recover reasons that were never returned.

### Change-request cost

| Request | Assessment | Required changes |
|---|---|---|
| **CR-1** | **Accommodates with effort** | Update `EligibilityRules.withinCreditLimit` to distinguish graduate and undergraduate students. Add approval lookup to `EnrollmentRequest` or a context service, a typed advisor-approval-required reason, and a rendering case in `ViolationMessages`. |
| **CR-2** | **Accommodates with effort** | Add a student-specific waiver record with grantor/time, make waiver state another explicit `RuleOutcome` (for example `Waived`), and update `EligibilityDecision.allowed()`/report rendering to consider only unwaived violations as blockers. With D3 fixed, waivers can also target individually identified outcomes. |
| **CR-3** | **Accommodates with effort** | Add an effective-enrollments view to `EnrollmentRequest` that excludes the proposed drop, then use it in `creditsAfterEnrolling` and `noScheduleConflict`. Evaluate before and after with typed outcomes and report what disappears. `Domain.java` does not need to change. |

### Refactoring sketch

I would preserve most of this design because its data is clearer than the other submissions. `RuleOutcome` identifies whether a rule passed or failed, `ViolationReason` has typed details instead of only an English message, `ViolationMessages` keeps display wording outside the rule calculations, and `EligibilityDecision` copies its list. These choices will help when the advising screen grows. I would first change the result contract so each rule can return zero or more typed findings, rather than exactly one outcome. For example, the prerequisite check would return a missing-prerequisite finding for each missing course, and the schedule check would return a separate conflict for each conflicting enrolled offering. The aggregate decision would still retain all results and preserve rule identities. Then I would add an evaluation context that exposes the effective enrollment list. Credit and schedule checks would read from that context so the normal and hypothetical-drop views use consistent data without changing `Student`. The graduate credit rule would consult written advisor approval using a dependency available through that context; graduate approval would remove the 12-credit restriction without bringing back the undergraduate 18-credit cap. A waiver would be a typed outcome holding the original reason plus grantor and timestamp, so the UI can clearly show *waived* instead of treating it as passed or hiding it. I would deliberately leave `Domain.java`, the existing grade/meeting helpers, and the general separation between eligibility calculations and message rendering unchanged. I would also avoid introducing inheritance for the rules: the current functional interface is enough once its output shape is fixed.

---

## Overall ranking: most to least maintainable

| Rank | Submission | Reasoning |
|---|---|---|
| **1** | **D — EligibilityRules** | Clear typed reasons, explicit pass/fail outcomes, a read-only decision list, and separate message rendering make future requests easier to reason about. Its important defect is the single outcome per rule; that must be fixed to report every reason, but the surrounding design is worth keeping. |
| **2** | **C — EnrollmentChecker** | Each check has its own implementation, and the checker gathers failures from all five rules. Sorting the real list, broken `hashCode`, and mutable decisions are serious, but those can be fixed locally before extending the result type. |
| **3** | **B — EligibilityEngine** | It currently evaluates without enrolling anyone and can collect multiple problems, so it is safer for advising than A. However, its visitor owns almost all real rule logic and adding a rule forces changes across multiple components. The large design offers less actual flexibility than it suggests. |
| **4** | **A — RegistrationService** | It looks simple, but it returns after the first failure, uses shared mutable errors, and silently changes enrollment data even when the UI only wants a description. Those behaviors require separating evaluation from registration before CR-2 or CR-3 can work safely. |

**Why this order:** The ranking is about how safely the system can answer today's question and how many responsibilities must change for the three new requests—not how many patterns a submission uses. B has more patterns than C or D, but they do not localize the policy. A has the fewest types, but its eligibility query can change real registrations. D is not automatically correct because it has typed records; it still hides multiple failures within a rule. That is why D needs a real correction even though I rank it first.

## Final implementation priorities (review only; not part of this assignment)

1. Preserve all failures and make eligibility checking side-effect-free.
2. Represent each issue with a stable rule ID and useful details; do not rely on parsing display text.
3. Treat written advisor approvals and registrar waivers as external, auditable records—not new fields in the supplied `Domain.java` types.
4. Run normal and hypothetical-drop checks against separate enrollment views, then compare the results.

**No production code was changed as part of this review.** The five supplied Java files belong in their isolated `src/projects/design-review/` folder and should not be mixed into `src/registration/` or compiled together with the semester implementation.
