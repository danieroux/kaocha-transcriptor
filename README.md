# What is this

A [Koacha](https://github.com/lambdaisland/kaocha) plugin for [Transcriptor](https://github.com/cognitect-labs/transcriptor)

# How do I use it

The `example` directory has a complete setup. After checking this out run:

`cd example && bin/kaocha`

## Quick setup

Your tests.edn needs to look like this:

```clojure
{:kaocha/tests                       [{:kaocha.testable/type :danieroux.type/transcriptor
                                       :kaocha.testable/id   :transcriptor
                                       :kaocha/test-paths    ["test"]}]
 :kaocha/color?                      true
 :kaocha/reporter                    [kaocha.report/dots]}
```

The `:kaocha/test-paths` contains the `.repl` files you want to run.

# I have questions and suggestions

Reach me as @danieroux on Clojurians