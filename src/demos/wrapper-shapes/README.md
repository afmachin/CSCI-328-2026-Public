# wrapper-shapes

**What it shows.** That Adapter is distinguishable from Decorator and Proxy *in the
source*, and that Decorator and Proxy are not distinguishable from each other at all.
It builds all three over one interface and prints what each implements against what each
holds:

```
Adapter    implements TranscriptSource, holds a LegacySisClient   -> same type? false
Decorator  implements TranscriptSource, holds a TranscriptSource   -> same type? true
Proxy      implements TranscriptSource, holds a TranscriptSource   -> same type? true
```

It then runs a three-deep stack twice and shows the audit line appearing **once**,
because the proxy declined to delegate on the second call.

**Cited by** Structural Design Module on canvas, *Adapter*;

**The companion exhibit** is `../adapter-not-assignable/`, which does not compile on
purpose: the adapter's inner object cannot stand in for the adapter, and that is a
compiler error rather than a matter of taste.

**Run it**

```sh
cd demos/wrapper-shapes
javac WrapperShapesDemo.java && java WrapperShapesDemo
```

Compiles clean. Output is identical on every run; `output.txt` is the recorded copy.
