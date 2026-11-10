# Triage: the context document

You have inherited `WaitlistManager`.

**The copy in this folder is standalone.** It is a later version of the class than the
one in the baseline, with its own constructor and its own collaborators, so
do not drop it into the baseline as it will not compile there. Read it, and work on it,
on its own. You have two days before the term rollover freeze, and you may fix **one**
thing.

## Change history

```
$ git log --oneline --since="3 years ago" -- WaitlistManager.java

a4f21c9  2026-08-14  Tune promotion throttle for the fall rush
7e0b3d1  2026-08-02  Fix CSV export column order for the registrar
1c9f8a2  2026-01-19  Add exportCsv for compliance reporting
b3d7e40  2025-08-11  Increase default promotion cap 25 -> 50
9a1c2f8  2025-03-04  Add formatWaitlistForAdvising
44e0b71  2024-08-19  Handle null entry list in positionOf
2f8d1a3  2024-08-06  Initial throttle implementation
```

Seven commits in three years. Five of them fall in August or the weeks before a term
starts.

## What depends on this

- `RegistrationController` — calls `addToWaitlist`, `removeFromWaitlist`, `positionOf`
- `SeatReleaseJob` — calls `promoteNext` on every drop
- `AdvisingScreen` — calls `formatWaitlistForAdvising`, `waitlistSize`
- `ComplianceExportJob` — calls `exportCsv`
- `NightlyBatch` — calls `clearDailyPromotions`, `purgeExpiredOffers`

Five callers. `WaitlistEntry` is used nowhere outside this file.

## Open tickets

**REG-4471 (committed for October).** Departments want their own promotion caps and
notification wording. Currently both are global.

**REG-4488 (reported, unreproduced).** The registrar reports that the daily promotion
throttle "sometimes lets too many through." Reported three times over two years,
never reproduced in staging, closed twice as not-reproducible.

**REG-4502 (backlog, unscheduled).** Advising wants the waitlist display to show
estimated wait time. No target date.

## Other notes

- No automated tests for this class. Verification is manual, through the registration
  UI.
- The term rollover freeze begins in two days. Only correctness fixes are permitted after
  it starts, and nothing you plan for "after the freeze" is in scope here.
- You are the only person working in this file.

---

Nothing above is background. Every line of it is evidence for a verdict.
