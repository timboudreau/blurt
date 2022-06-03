// Generated by com.dv.sourcetreetool.impl.App
open module com.mastfrog.blurt {
    exports com.mastfrog.blurt;

    // derived from de.undercouch/bson4jackson-2.13.1 in de/undercouch/bson4jackson/2.13.1/bson4jackson-2.13.1.pom
    requires transitive bson4jackson.2.13.1;

    // derived from com.mastfrog/cluster-0.0.0-? in com/mastfrog/cluster/2.6.13/cluster-2.6.13.pom
    requires transitive cluster.2.6.13;

    // Inferred from source scan
    requires com.mastfrog.collections;

    // Sibling com.mastfrog/giulius-3.0.0-dev

    // Transitive detected by source scan
    requires com.mastfrog.giulius;

    // Sibling com.mastfrog/giulius-annotations-3.0.0-dev
    requires com.mastfrog.giulius.annotations;

    // Sibling com.mastfrog/giulius-settings-3.0.0-dev
    requires com.mastfrog.giulius.settings;

    // Inferred from source scan
    requires com.mastfrog.misc;

    // Inferred from source scan
    requires com.mastfrog.preconditions;

    // Inferred from source scan

    // Transitive detected by source scan
    requires com.mastfrog.streams;

    // Inferred from source scan
    requires com.mastfrog.strings;

    // derived from com.fasterxml.jackson.core/jackson-databind-0.0.0-? in com/fasterxml/jackson/core/jackson-databind/2.9.9.3/jackson-databind-2.9.9.3.pom
    requires transitive jackson.databind;
    requires java.logging;

    // Inferred from test-source-scan
    requires transitive junit;

}