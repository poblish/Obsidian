Feature: Static test pages

  Scenario: Ipsum
    When one visits the ipsum page
    Then the title must say "HTML Ipsum Presents"
    And the text should contain "Pellentesque habitant morbi tristique"

  Scenario: OK Links
    When one visits the ok_links page
    Then the title must say "HTML Ipsum Presents"
    And a link should say "Google"
    And a link should say "OK"
    And no link should say "Blah"
