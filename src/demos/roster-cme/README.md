# roster-cme

**What it shows.** Iterator's misuse signature in the baseline's own `Roster`, next to the
version on pattern card 04, so the difference is one of design rather than diligence.

`BaselineRoster` is `codebase/v0-baseline/src/registration/Roster.java` transcribed — a
private `HashMap` and a getter that returns it. `CardRoster` is card 04's shape.

```
--- the baseline's Roster ---
getStudents() returns the live map, so a caller can drop a
student mid-traversal. Nothing in Roster's source forbids it:
  visited 1 of 3, then java.util.ConcurrentModificationException
```

The sentence to land is the one after it: **the exception is the lucky case.**
`ConcurrentModificationException` is thrown on a best-effort basis, so the same code can
also just skip a student and return normally. A roster that silently omits somebody is
worse than one that crashes, and the baseline's design permits both.

```
--- card 04's Roster ---
for-each works: Alvarez Brennan Chau
stream works:   S1 S2 S3
remove() through the iterator: UnsupportedOperationException
```

The caller gets traversal and nothing else, which is the whole of what the pattern is
for. Note that card 04's version is not merely more careful — it is a different decision
about what the type promises.

**On the removal.** The student dropped mid-traversal is chosen as "the first key that is
not the current one", so the demo does not depend on `HashMap`'s iteration order. Which
student that turns out to be *does* depend on it, and that is part of the point: the
caller does not control the order and cannot reason about what it just skipped.

**Cited by** `notes/09-behavioral-ii.md`, *Iterator: the clearest version of the theme*;
card 04's Iterator misuse signature; and Exercise 11's package-boundary argument, where
`getStudents()` is one of the wrong-way arrows.

**Run it**

```sh
cd reference/demos/roster-cme
javac RosterCmeDemo.java && java RosterCmeDemo
```

Compiles clean under `-Xlint:all`. Output is identical on every run; `output.txt` is the
recorded copy.
