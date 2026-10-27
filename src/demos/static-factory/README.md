# static-factory

**What it shows.** The three things a static factory method can do that a constructor
cannot, each as a result you can read off the screen rather than take on trust:

- two factories can share a parameter list where two constructors cannot;
- a factory may return an instance that already exists (`Integer.valueOf(127) ==
  Integer.valueOf(127)` is `true`, `(128)` is `false`);
- a factory may return a subtype you cannot name (`List.of(1, 2)` is a `List12`).

**Cited by** Creational Design Module on canvas, *Static factory methods*. The note quotes four
results from here by name and tells you to run it rather than believe the two
`Integer.valueOf` lines, which look like a trick and are not.

**Run it**

```sh
cd demos/static-factory
javac StaticFactoryDemo.java && java StaticFactoryDemo
```

Compiles clean. Output is identical on every run; `output.txt` is the recorded copy.
