open module com.mastfrog.blurt {
// Generated by com.dv.sourcetreetool.impl.App

    exports com.mastfrog.blurt;

    // derived from com.fasterxml.jackson.core/jackson-databind-2.13.2.2 in com/fasterxml/jackson/core/jackson-databind/2.13.2.2/jackson-databind-2.13.2.2.pom
    requires transitive com.fasterxml.jackson.databind;

    // derived from com.mastfrog/cluster-2.6.13 in com/mastfrog/cluster/2.6.13/cluster-2.6.13.pom
    requires transitive com.mastfrog.cluster;
    requires transitive com.mastfrog.giulius;
    requires transitive com.mastfrog.giulius.annotations;
    requires transitive com.mastfrog.giulius.settings;

    // derived from de.undercouch/bson4jackson-2.13.1 in de/undercouch/bson4jackson/2.13.1/bson4jackson-2.13.1.pom
    requires transitive de.undercouch.bson4jackson;
    requires java.logging;

    // derived from junit/junit-4.13.2 in junit/junit/4.13.2/junit-4.13.2.pom
    requires transitive junit.junit;

}
