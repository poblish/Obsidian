# Design Decisions

## A DSL, not a 'proper' language

Why use Obsidian when I can implement WebDriver, HTTP, assertions etc., in a proper language like Java?

* We believe that the essence of the test is of primary importance: the test strategy, the data, the steps, the
assertions and validation, and that any source code required to implement that is second-class 'support' code.
It's likely to be far more verbose than the test itself, subject to bloat and refactoring,
written by non-specialist developers, and more likely to degrade rapidly. By using Obsidian you delegate the task
of maintaining that source code to us.
* Optimum use of web testing APIs, code style etc. should not be important for test support code. Though the power
of a real language is tempting, a clear and well-defined DSL lets you focus on the testing task in hand rather than
the many ways in which first-class developers might implement a superior solution.

In short, we offer a DSL that avoids any clash of skill or style between developers and testers.
and that is fluent enough for both developers and testers to get equally productive with.

## Check, don't drive

Though not ruthlessly enforced, our philosophy is to discourage mere 'driving' of a web site from the start to the
end of a journey - though this is a check of sorts - and to encourage a high percentage of assertions within scenarios,
so that every action is checked, that all lookups are checked, and there are no mere 'getters'.

## Deterministic tests

Test scenarios should be fixed and stable, just as test datasets and (properly working) systems-under-test (SUT) should be,
to ensure overall consistency and honesty. The tester defines expectations, the SUT must fulfil them.

We achieve this by:
* Looping, though allowed, can only be done across fixed ranges. It can't be influenced by the SUT, e.g. it's not
possible to validate all `<p>` on a page, only those a tester defines.


## No parallel running of test scenarios

This is not forbidden, but there are many ways to parallelise a test run, and you may well prefer to split jobs
across different runners / agents for cleaner separation, rather than within a test framework runner like Obsidian
itself. However, this may change.