module com.github.robtimus.obfuscation.jackson.databind {
    requires transitive com.github.robtimus.obfuscation;
    requires transitive com.github.robtimus.obfuscation.annotations;
    requires transitive com.fasterxml.jackson.databind;

    exports com.github.robtimus.obfuscation.jackson.databind;

    opens com.github.robtimus.obfuscation.jackson.databind to com.fasterxml.jackson.databind;
}
