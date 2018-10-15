# Obsidian

A new black-box testing framework, written in Kotlin.

Test scenarios are YAML files, ensuring equal accessibility for developers and testers.

Obsidian is an integration of existing web-based technologies, combined with robust assertions, all accessible via common DSL.


#### Integrated technologies:

* WebDriver via [Selenium 3.x](https://github.com/SeleniumHQ/selenium)
  * Local browsers, or via local/remote Selenium Grid
  * Headless or windowed
* Assertions/matchers from [Hamkrest](https://github.com/npryce/hamkrest)
* Java [Unified Expression Language](https://en.wikipedia.org/wiki/Unified_Expression_Language) implementation for `${}` expressions
* HTTP requests and lookups via [HttpClient](https://hc.apache.org/httpcomponents-client-ga/)
* Capture and query HAR files, or throttle traffic using [BrowserMob Proxy](https://github.com/lightbody/browsermob-proxy)
* API testing via [WireMock](https://github.com/tomakehurst/wiremock)
* [Cucumber](https://github.com/cucumber/cucumber-jvm) tests
* Pull Docker images and launch test containers
* Database commands and lookups for data-driven tests (MySQL and Postgres)
* Amazon S3 and Redis commands and lookups
* XML / JSON / JavaScript / Cookies

#### Goals:

* Simple commands for the lightest, most maintainable tests
* YAML for ease of reading, editor support, linters, consistent syntax
* Deterministic tests: perform operations but check / assert everything


---
## Examples

1. A **cross-browser** test:

        browsers: {chrome,phantomjs,htmlunit,firefox,safari 12}

        Simple content checks:
        - url: local:demos/static_pages/ipsum.html
        - waitFor: {that: p, contains: Pellentesque habitant morbi}

    That test will look for local browsers unless configured for a Selenium Grid.

1. A **Google search** test, using the default browser (headless Chrome):

        Google test:
        - url: http://www.google.co.uk
        - type: rembrandt van rijn wikipedia return
          in: input[type=text]
        - waitFor: {that: '#rso h3', equals: 'Rembrandt - Wikipedia'}  # First match

1. Screenshots:

        browsers: firefox  # FF reliably handles element screenshots

        Element and page screenshot example:
        - url: local:demos/static_pages/ipsum.html
        - screenshot: {as: NewFullScreenScreenshot}
        - screenshot: {of: h2, as: Header2Screenshot}

1. Matchers (textual or numeric):

        - assert: {that: GitHub,
                   eq: GitHub,
                   anyOf: {GitHub, GitHub, orSomethingElse},
                   equalsIgnoreCase: {giThub, github, GitHuB},
                   not eq: github,
                   not equalsIgnoreCase: xxx,
                   contains: {itHub, Git},
                   containsIgnoreCase: {ithub, th, ub},
                   containsIgnoreWhitespace: {'  tHub', 'Gi  ', ub},
                   not contains: {xxx, Blah},
                   startsWith: GitH,
                   startsWithIgnoreCase: {gith, gi},
                   endsWith: tHub,
                   endsWithIgnoreCase: thub,
                   matches: 'G\S+ub',
                   matchesIgnoreCase: thub,
                   not matches: 'G\s+ub'}

        - assert: {that: 123345,
                   eq: 123345,
                   not equals: 9876543,
                   gt: 1,
                   not gt: 9876543,
                   gte: {1,123345},
                   lt: 9876543}

---

## Runnning Scenarios

Run with GUI:

    ./gui.sh

Run without GUI, with local web browsers:

    ./local.sh demos/google.yaml demos/fixme.yaml ...

    ./local.sh demos/  # All in a folder...

Run using a local Selenium Grid:

    ./grid_local.sh demos/google.yaml ...

Run using a remote Selenium Grid:

    ./grid_remote.sh host:port demos/google.yaml ...

Longer form, showing global defaults and overrides (enforce headless Chrome-only):

    java -jar dist/obsidian.jar \
         --nogui \
         --defaultConfig="{systemProperties: {env: Test}, defaults: {window: {width: 1200}}}" \
         --overrideConfig="browsers: chrome" \
         demos/google.yaml

---

## Runnning Cucumber Scenarios

Run Cucumber (specifying list of scenarios, and list of implementations):

    ./cuke.sh [gherkin scripts or folders] [scenarios or folders]
    ./cuke.sh test.feature test.yaml