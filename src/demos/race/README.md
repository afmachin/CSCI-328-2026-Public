# race

**What it shows.** That the baseline's seat count can be wrong. `CourseOffering` holds
`enrolledCount`, `Student.enroll` increments it, and the eligibility check reads
`hasOpenSeats()` somewhere else entirely — so two threads can both be told there is a
seat. That is the shipped design, not a contrived one.

**Cited by** data-oriented module, on invariants with two owners.

**Run it**

```sh
cd demos/race
javac RaceDemo.java && java RaceDemo
```

Compiles clean. **This program is nondeterministic by nature, so `output.txt` is a
recording rather than a target to match.** It holds a short provenance header and then
**two consecutive runs** of the identical program on the identical machine — because the
two runs differing is itself the demonstration. Your numbers will differ from both, and on
a single-core machine or an unlucky run a section may report zero failures. **Zero
failures is not a passing grade** — it means the window was not hit this time.

Two things in the recording are not the program's own output: the header, and a
`Picked up JAVA_TOOL_OPTIONS:` line the recording machine's JVM printed about its
environment. You will not see the latter.
