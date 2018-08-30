# Ad libs in Clojurescript

Example of how to use `deps.edn` and a simple build script (`build.clj`) to 
manage a cljs project rather than leinigen or boot. Structure heavily inspired
by [Figwheel with new Clojure 1.9 CLI tools](http://www.functionalbytes.nl/clojure/nodejs/figwheel/repl/clojurescript/cli/2017/12/20/tools-deps-figwheel.html). This is much simpler and easier to reason
about then a more complex lein or boot setup. Any custom workflow can be
created since it's just code, no plugins, no magic.

The example app is a simple reagent "mad libs"-like fill in the blanks app. I
wasn't feeling particularly creative so it just uses some "lorem ipsum" text
as a placeholder. It demos how to do keyboard navigation with
[reagent](http://reagent-project.github.io/)

Try it out here: <https://demos.pauldlug.com/adlibs/>

* Enter move to next text input and reveals the hidden text if all fields have
  been filled out.
* Up/down arrows move to next or previous text input
* Escape clears all text and resets state


## Usage

In development run:

```bash
clj -A:figwheel -m figwheel.main -b dev -r
```

Then open a browser to <http://localhost:3449/> and you'll have a nice figwheel
instance.

For a production build run:

```bash
clj -A:figwheel -m figwheel.main -bo prod
```

## Deployment

Just write a script! Like this one:

```bash
#!/usr/bin/env bash

rm -rf resources/public/js/compiled
clojure build.clj compile
aws s3 sync resources/public/ s3://BUCKETNAME/PATH/
```
