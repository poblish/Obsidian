package org.hiatusuk.obsidian.run.profiles

object Profiles {

    fun shouldRunScenario(specifiedProfiles: Collection<String>, scenarioProfiles: Collection<String>): Boolean {
        if (specifiedProfiles.isEmpty() || scenarioProfiles.isEmpty()) {
            return true
        }

        var gotMatch = false

        for (eachSpec in specifiedProfiles) {
            if (eachSpec.startsWith("-")) {
                return !scenarioProfiles.contains(eachSpec.substring(1))
            } else if (scenarioProfiles.contains(eachSpec)) {
                gotMatch = true
            }
        }

        return gotMatch
    }
}
