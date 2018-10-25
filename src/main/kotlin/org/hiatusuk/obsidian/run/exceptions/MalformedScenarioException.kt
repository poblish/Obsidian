package org.hiatusuk.obsidian.run.exceptions

class MalformedScenarioException(value: Any) : RuntimeException("Malformed scenario: $value")