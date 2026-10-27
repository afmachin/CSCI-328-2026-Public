# adapter-not-assignable

**DOES NOT COMPILE, on purpose. The failure is the demonstration.**

**What it shows.** A decorator's inner object is interchangeable with the decorator —
both are the wrapped interface. An adapter's inner object is not, and the compiler says
so:

```
error: incompatible types: LegacySisClient cannot be converted to TranscriptSource
```

That is why Adapter is the one structural pattern a reader can identify without reading
its name.

**Cited by** `notes/07-structural.md`, *Adapter: the one you already have*, and the Week
7 session opening. The runnable companion is `../wrapper-shapes/`.

**Run it**

```sh
cd reference/demos/adapter-not-assignable
javac AdapterNotAssignable.java
```

`javac` must **fail**. The recorded answer is `javac-output.txt`. This is the only
`.java` in the folder, so nothing else breaks when this one does — which is why it is a
real `.java` file and not a `.txt` you have to rename first.

The recorded error carries a line number, so editing the file shifts it. Re-record if
you do.
