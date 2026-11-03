# chain-order

**What it shows.** That a Chain of Responsibility's handler order *is* the policy, and
that the policy is written down nowhere except the order somebody built the list in.

Three authorities over a registration override — department chair (prerequisite waivers
only), dean (credit overloads up to three credits), registrar (anything at all). One
request: a two-credit overload. Two assemblies of the same three handlers:

```
chain built chair -> dean -> registrar
  granted by: dean
chain built chair -> registrar -> dean
  granted by: registrar
```

Same handlers, same request, different authority. No handler was edited between the two
runs, and no handler can tell you which list it is in.

The registrar is the one that makes position load-bearing, and that is realistic: a
catch-all authority at the end of a chain is a fallback, and the same authority moved one
place forward silently takes every decision from everyone behind it.

**Cited by** `notes/09-behavioral-ii.md`, *Chain of Responsibility: order is invisible and
load-bearing*.

**The companion** is `../chain-falls-off/`, which is the other half of the same section:
what happens when no handler claims the request at all.

**Run it**

```sh
cd reference/demos/chain-order
javac ChainOrderDemo.java && java ChainOrderDemo
```

Compiles clean under `-Xlint:all`. Output is identical on every run; `output.txt` is the
recorded copy.
