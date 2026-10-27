# wrapper-identity

**What it shows.** What wrapping costs a caller, in three parts.

1. **Identity.** `wrapped == rule` is false and `wrapped instanceof CreditLimitRule` is
   false, so a check that worked before the wrapper went in now silently says no.
2. **Equality.** `wrapper.equals(inner)` and `inner.equals(wrapper)` are **both** false.
   That is ordinary inequality, not the symmetry break note 03 warns about — a point the
   note makes carefully, because the two are easy to confuse. A set holding the rule and
   its wrapper still has size 2.
3. **A real stack trace** through four wrappers, printed rather than described. Seven
   frames, five of them named `evaluate`, and it reads **inside-out**: you write
   `Retried` first and it appears last, just above `main`.

**Cited by** Structural Design Module on canvas

**Run it**

```sh
cd demos/wrapper-identity
javac WrapperDemo.java && java WrapperDemo
```

Compiles clean. Output is identical on every run; `output.txt` is the recorded copy.
The stack-trace line numbers in `output.txt` are real, so editing `WrapperDemo.java`
shifts them — re-record if you do.
