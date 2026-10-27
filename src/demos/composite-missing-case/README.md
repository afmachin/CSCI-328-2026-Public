# composite-missing-case

**DOES NOT COMPILE, on purpose. The failure is the demonstration.**

**What it shows.** What the sealed Composite costs on the axis it does not win. A fourth
node type — `MinimumGrade` — is added to the sealed interface and the existing `switch`
is left exactly as it was in `../composite-shapes/`. The compiler refuses:

```
error: the switch expression does not cover all possible input values
```

This is the expression problem in four lines of diff. The sealed shape makes a new
*operation* cheap and a new *node type* expensive; the classical shape is the other way
round. Neither is an upgrade on the other, which is why note 07 calls it a trade.

**Cited by** Structural Module on Canvas, *The sealed shape*. Week 10 gives this its proper
treatment.

**Run it**

```sh
cd demos/composite-missing-case
javac CompositeMissingCase.java
```

`javac` must **fail**. The recorded answer is `javac-output.txt`. This is the only
`.java` in the folder, so nothing else breaks when this one does.

The recorded error carries a line number, so editing the file shifts it. Re-record if
you do.
