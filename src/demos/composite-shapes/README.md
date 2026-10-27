# composite-shapes

**What it shows.** Composite written twice against the same prerequisite rule — *CS201,
and either MATH150 or MATH160* — so the two shapes can be compared on identical work.

- **The 1994 shape**, transparent: `add` and `remove` on the shared type. Part one ends
  in the `UnsupportedOperationException` the design forces on its leaves. That throw is
  the transparent design working as intended, not a bug in the pattern.
- **The sealed shape**: three records, no `add`, no `remove`, operations as an exhaustive
  `switch` with no `default`. Nothing throws and nothing mutates.

**Cited by** Structural Design Module on canvas, *Composite is not like the others*.

**The other two parts of this story** are next door: `../composite-trade/` prices the
safe design, and `../composite-missing-case/` shows what the sealed shape costs when a
fourth node type arrives.

**Run it**

```sh
cd demos/composite-shapes
javac CompositeDemo.java && java CompositeDemo
```

Compiles clean. Output is identical on every run; `output.txt` is the recorded copy.
