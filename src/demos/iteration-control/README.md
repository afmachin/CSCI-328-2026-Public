# iteration-control

**What it shows.** The four things the caller gives up when it hands the loop to the
collection. Each is something that was trivial while the caller owned the loop.

**1. Stopping early.**

```
external, stop at the first student over 16 credits: visited S1 S2 S3 -> stopping at Chau
internal, forEach with the same test:              visited S1 S2 S3 S4 S5 -> all five, no way out
```

Worth being precise here, because the usual version of this claim is too strong: a
`Stream` *can* short-circuit, but only through an operation built to — `findFirst`,
`anyMatch`, `takeWhile`. `forEach` is not one of them, and the demo shows `findFirst`
working immediately below the `forEach` that cannot.

**2. Stepping two sequences together.** Two iterators advanced in one loop body pairs
students with advisors with no index arithmetic anywhere. `forEach` hands each lambda one
element and no position, so the same pairing needs an external counter — which is the
loop variable the caller just gave away.

**3. Resuming.** An iterator is a position, so it can be put down and picked up: read two
students, go and do something else, come back and finish. `forEach` has no position to
hand back.

**4. Checked exceptions** — this one is a compile error, and `CheckedInLambda.java.txt`
is the exhibit. `Consumer.accept` declares no checked exception, so an `IOException`
thrown inside a lambda has nowhere to go, and adding `throws IOException` to the
enclosing method does not help because the lambda is a different method:

```
CheckedInLambda.java:37: error: unreported exception IOException; must be caught or declared to be thrown
        ROSTER.forEach(s -> writeTranscript(s));
                                           ^
1 error
```

The same call in a `for` loop, four lines up in the same file, compiles — the file
contains both so the contrast is in one compilation. Recorded in `javac-checked.txt`.
It keeps the `.java.txt` spelling because it shares this folder with a demo that must
compile, so it has to stay invisible to `javac *.java`.

**Cited by** `notes/09-behavioral-ii.md`, *Iterator: the clearest version of the theme*,
and self-check 7.

**Run it**

```sh
cd reference/demos/iteration-control
javac IterationControlDemo.java && java IterationControlDemo

cp CheckedInLambda.java.txt /tmp/CheckedInLambda.java && javac /tmp/CheckedInLambda.java
```

The demo compiles clean under `-Xlint:all` and its output is identical on every run;
`output.txt` is the recorded copy. The exhibit is supposed to fail.
