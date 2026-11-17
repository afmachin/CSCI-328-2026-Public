# refactoring-steps

**What it shows.** Note 12a's *Working it through* as four runnable states of the same
class, with a pin harness between them. Step 0 records the behaviour and finds a defect by
running the code twice. Step 1 adds the seam. Step 2 extracts the rules and hits the friction
the note predicts. Step 3 changes the behaviour, and exactly one pin breaks.

**Each step ships its own service class** — `Step0Service`, `Step1Service`, `Step2Service`,
`Step3Service` — so the four can be read beside each other and the diff between them is the
lesson. They share the domain classes above them, transcribed from
`src/registration` and trimmed to what these four exercise.

## Step 0 — the pin, and what pinning finds

```
      enrollments before:     0, seats taken: 0
      after 1 describeResult: 1, seats taken: 1
      after 2 describeResult: 2, seats taken: 2
      capacity is 2; open seats? false
```

One student, asked about twice, has consumed a two-seat section. **The act of pinning found
a defect that reading did not**. Two defects meet there: the query mutates, and nothing checks whether a
student is already enrolled.

**The offering must have no `MeetingTime` on it**, and the demo shows why by doing it both
ways. With meetings — which is how `Main` builds every offering in the baseline — the first
call enrolls her and the second trips the time-conflict rule against *her own enrollment*:

```
      second call: Alice Reyes could not register: / Time conflict with CS201
      seats taken: 1 -- the time-conflict rule caught her own enrollment.
```

Same defect underneath and the seat count never moves. This is the
fixture detail that decides whether the demonstration lands.

## Step 1 — the seam, priced in pins

```
      pass   clean enrollment
      ...
  0 pins broken. Commit.
```

Then the thing the seam actually bought, which Step 0 could not do at all: a student the SIS
has never heard of, registered against a `FakeGradeSource`. That is the whole argument for
dependency injection, and it is not an argument about testing.

Note the constructors. `RegistrationService` in the baseline declares no constructor, so it
has the implicit default; declaring the parameterized one removes it and every
`new RegistrationService()` stops compiling. The no-argument constructor in `Step1Service` is
written back by hand for exactly that reason.

## Step 2 — the friction is the finding

Behaviour unchanged, nothing broken, and this signature:

```java
private boolean checkPrerequisites(String[][] rows,
                                   CourseOffering offering,
                                   List<String> errors)
```

Each rule must report two things — that it failed, and why — and a `boolean` carries one of
them, so the error list goes in as a parameter the method only ever appends to. **That
parameter is not a wart in the refactoring.** It is the design saying that a rule's result is
a value nobody modelled, and it is the same reason the caller cannot report more than one
failure.

## Step 3 — one pin breaks, and four cannot tell

```
      pass   clean enrollment
      pass   missing prerequisite
      pass   over the credit cap
      pass   section full
      BREAK  two failures at once
             recorded: Dana Vale could not register: / Missing prerequisite: CS101
             now:      Dana Vale could not register: / Missing prerequisite: CS101 / Would total 20 credits, limit is 18
```

**Four pins passing was never evidence that the
change worked — it was evidence that four of the five could not tell.** A case built to fail
one rule reports one rule whether you return early or collect, so only the fifth case can
observe this change, which is why Step 0 recorded it. 

## What was trimmed, and what was not

The domain classes keep the baseline's shapes exactly where it matters: `Student.enroll`
mutates both sides, `CourseOffering` carries a seat count the service increments,
`LegacySisClient` is a singleton with a private constructor, and the five rules run in the
baseline's order with the baseline's messages.

Trimmed: the waitlist branch of the full-section rule, so that case reports `Section is full`
— which is the baseline's own message on the other side of that branch, when
`waitlist_enabled` is off. `Config` is reduced to the one key `register` reads. The
transcript, the audit log and the notification gateway are gone, since no rule reads them.

The time-conflict rule is present in all four services and no pin exercises it; the only
place it fires is Step 0's with-meetings aside. It is the rule that makes
the aside work, and pinning it would have required a case the note does not ask for.


**Run it**

```sh
cd reference/demos/refactoring-steps
javac RefactoringStepsDemo.java && java RefactoringStepsDemo
```
