# lambda-identity

**What it shows.** Why a lambda cannot be reliably unsubscribed, which is the sharpest
single result in the pattern block.

- What a lambda is actually **called** at runtime:
  `LambdaIdentityDemo$$Lambda/0x...@...` — no method name, nothing worth printing.
- **Non-capturing** lambdas are cached, so the same expression evaluated twice gives the
  same object. **Capturing** lambdas are not: every evaluation is a new object. Every
  realistic listener captures something, which is why the Observer failure is reliable
  rather than occasional.
- Two lambdas with character-for-character identical bodies are not `==` and not
  `equals`.
- Inside a lambda `this` is the **enclosing instance**, not the lambda — so a lambda
  written in an instance method captures the whole enclosing object.

**Cited by** Functional Collapse Module on canvas, *Observer: it does not collapse at all*.

**Run it**

```sh
cd reference/demos/lambda-identity
javac LambdaIdentityDemo.java && java LambdaIdentityDemo
```

Compiles clean. **Two lines will not match `output.txt` and are supposed not to:** in
part one the `/0x...` class address is assigned by the JVM and differs on every run, and
the `@...` identity hash can differ between JVMs. Everything else is reproducible —
parts two to five printed identically across runs on JDK 21.0.12. 