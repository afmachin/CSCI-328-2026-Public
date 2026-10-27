# no-function-types

**DOES NOT COMPILE, on purpose. The failure is the demonstration, and there are two of
them.**

**What it shows.** That Java — including Java 21 — has no function types.

```
error: incompatible types: WaitlistPolicy cannot be converted to Priority
error: cannot infer type for local variable ranker
  (lambda expression needs an explicit target-type)
```

The first is two interfaces with one identical method each: same shape, different types,
not interchangeable, because Java matches types by **name** and not by shape. The second
is a lambda with nothing to be assigned to — **a lambda has no type of its own.** Its
type is whichever functional interface it was handed to, and with no target there is no
type.

The second error is the one that matters later: it is why the Observer failure in
`../lambda-identity/` is not an accident of the library.

**Cited by** Functional collapse Module on canvas

**Run it**

```sh
cd demos/no-function-types
javac NoFunctionTypes.java
```

`javac` must **fail**, with two errors. The recorded answer is `javac-output.txt`. This
is the only `.java` in the folder, so nothing else breaks when this one does.

The recorded errors carry line numbers, so editing the file shifts them. Re-record if
you do.
