# chain-falls-off

**What it shows.** That falling off the end of a chain is not an error the chain can
make — it is a decision the *caller* makes, and makes by default if nobody thought about
it.

A chain of chair and dean meets a time-conflict override, which neither has authority
over. The chain returns `Optional.empty()`, which is honest. Then two callers:

```
--- caller A: treats empty as 'denied' ---
approved = false
tells the student: override denied
...which is a denial nobody issued.

--- caller B: treats empty as 'not my decision to make' ---
NoAuthorityException: no handler has authority over TIME_CONFLICT for S7
...which is the truth, and is now somebody's job.
```

The point worth drawing out: "nobody had authority" and "the request was denied" are
different facts, and an empty `Optional` is the same value for both. Caller A is not
obviously wrong on inspection — `isPresent()` reads like a decision — which is why this
one is worth running rather than describing.

**Cited by** `notes/09-behavioral-ii.md`, *Chain of Responsibility: order is invisible and
load-bearing*, and its *What this costs* entry on silent chains.

**The companion** is `../chain-order/`.

**Run it**

```sh
cd reference/demos/chain-falls-off
javac ChainFallsOffDemo.java && java ChainFallsOffDemo
```

Compiles clean under `-Xlint:all`. Output is identical on every run; `output.txt` is the
recorded copy.
