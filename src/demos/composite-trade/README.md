# composite-trade

**What it shows.** The price of the **safe** Composite design — `add` and `remove` on
the group only, so no node ever lies. The client that walks the tree now has to ask what
it is holding, and the demo counts the branches that costs.

Put beside `../composite-shapes/`, the pair is the whole trade GoF named: transparency
buys a uniform client and makes one kind of node lie; safety stops the lying and puts the
branching back in every client.

**Cited by** Structural Design Module on canvas, *The safe design, which is the other option*.

**Run it**

```sh
cd demos/composite-trade
javac CompositeTradeDemo.java && java CompositeTradeDemo
```

Compiles clean. Output is identical on every run; `output.txt` is the recorded copy.
