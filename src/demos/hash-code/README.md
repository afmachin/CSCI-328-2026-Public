# hash-code

**What it shows.** The `equals`/`hashCode` contract violation, in two parts.

1. **The mechanism**, on a small type: two objects that are `equals` with different hash
   codes, a `HashSet` that holds both, and a `contains` that says no.
2. **Your own codebase.** `MeetingTime`, copied verbatim from
   `registration/MeetingTime.java`, does exactly the same
   thing: two equal meeting times land in two distinct slots, so a combined week reports
   2 where an advisor expects 1.

Part two is the point. It is not a textbook hypothetical — it is the shipped baseline,
and you can run the same experiment against the real class whenever you like.

**Cited by** Contracts Design Module on canvas, which quotes part two's output and gives the
commands. Milestone 2 turns on the same defect.

**Run it**

```sh
cd demos/hash-code
javac HashCodeDemo.java && java HashCodeDemo
```

Compiles clean. Output is identical on every run; `output.txt` is the recorded copy.
