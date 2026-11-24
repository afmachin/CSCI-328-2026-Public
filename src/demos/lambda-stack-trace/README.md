# lambda-stack-trace

**What it shows.** What a stack trace from composed functions actually looks like, which is
narrower than the reputation. The comparison table used to say a lambda gives you
`Lambda$14`; it does not, and the real cost is somewhere else.

- The **rule that threw is named**: `Rules.seatsAvailable(Rules.java:9)`. A method reference
  traces to its real method, so finding the culprit is no harder than with a named class.
  (An inline lambda traces to `lambda$method$0` with a line number — also findable.)
- What is lost is the **path**. Each `and()` level contributes one frame of the same
  synthesized method, `EligibilityRule.lambda$and$0`, so a three-rule composition shows two
  identical-looking frames and ten would show ten. The trace tells you how deep you were and
  nothing about which rules you passed through on the way in.

That is the readability row arriving at three in the morning: composition is what goes
invisible, not the rule.

**Run it**

```sh
cd demos/lambda-stack-trace
javac -d out *.java && java -cp out Main
```

Compiles clean under `-Xlint:all`. Reproducible: the four lines of `output.txt` are the whole
output and they are identical on every run, because nothing here prints a JVM-assigned
address.

**Why this demo is five files and has no header comments.** It is the one demo that cannot be
collapsed into a single file. Nesting the types as static inner classes renames the frames to
`Demo$Rules.seatsAvailable`, and the point of the demo is the frame names. For the same
reason the sources carry no banner comment: note 14 quotes `Rules.java:9`,
`EligibilityRule.java:7`, `EligibilityRule.java:8` and `Main.java:8` verbatim, so inserting a
line anywhere above those would silently invalidate the note's own quoted output. 

The three unexercised rules (`prerequisites`, `creditLimit`, and `RuleOutcome.PASS`) exist to
make the composition three deep, which is what produces the two middle frames. One rule
would produce none.
