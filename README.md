# obfuscation-jackson-databind

Provides support for serializing and deserializing obfuscated values using Jackson. All you need to do is register a module:

    Module module = ObfuscationModule.defaultModule();
    mapper.registerModule(module);

This will automatically allow all instances of [Obfuscated](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscated.html) to be serialized and deserialized, without the need for any custom serializer or deserializer. In fact, any annotation used (apart from the ones below) will be used for the `Obfuscated` object's *value*, not the `Obfuscated` object itself. That means that you can provide custom serialization and/or deserialization for the value the way you're used to, without needing to wrap it inside a new serializer or deserializer.

## Annotation-based obfuscation

With the annotations from [obfuscation-annotations](https://robtimus.github.io/obfuscation-annotations), you can control how values are obfuscated during deserialization. This is supported for the following types:

### Obfuscated

* Use an annotation like `@ObfuscateFixedLength(3)` or `@ObfuscateAll` to specify a custom obfuscator to use for the `Obfuscated` property during deserialization.
* Use `@RepresentedBy` to provide a custom character representation.

### List, Set, Collection and Map

* Use an annotation like `@ObfuscateFixedLength(3)` or `@ObfuscateAll` to apply obfuscation to a `List`, `Set`, `Collection` or `Map` property during deserialization. This is done using [Obfuscator.obfuscateList](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscator.html#obfuscateList-java.util.List-java.util.function.Function-), [Obfuscator.obfuscateSet](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscator.html#obfuscateSet-java.util.Set-java.util.function.Function-), [Obfuscator.obfuscateCollection](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscator.html#obfuscateCollection-java.util.Collection-java.util.function.Function-) and [Obfuscator.obfuscateMap](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscator.html#obfuscateMap-java.util.Map-java.util.function.Function-) respectively.
* Use `@RepresentedBy` to provide a custom character representation for the generic element/value type. This annotation will be ignored if no obfuscation is applied to the property.

## Default obfuscators

By default, deserialized `Obfuscated` properties that are not annotated with any of the annotations from [obfuscation-annotations](https://robtimus.github.io/obfuscation-annotations) will use [Obfuscator.fixedLength(3)](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscator.html#fixedLength-int-). This can be overridden by using a builder to create the module. With this builder, it's possible to define default obfuscators per type, or a global default obfuscator:

    Module module = ObfuscationModule.builder()
            .withDefaultObfuscator(String.class, Obfuscator.portion().keepAtStart(2).build())
            .withDefaultObfuscator(Obfuscator.fixedValue("<obfuscated>"))
            .build();
    mapper.registerModule(module);

A type-specific obfuscator will be used if the generic value type of the `Obfuscated` property matches. This takes into account super classes and implemented interfaces. If there is no match, the global default obfuscator is used.

A type-specific obfuscator will be used not just for `Obfuscated` properties, but also any `List`, `Set`, `Collection` or `Map` property with a matching generic element/value type. This can be disabled by calling `requireObfuscatorAnnotation(true)` on the builder. The global default obfuscator will not cause any `List`, `Set`, `Collection` or `Map` to be obfuscated.

### Obfuscator lookup order

The following order is used to look up obfuscators for properties:

1. The obfuscator defined in the property's own annotations.
2. The default obfuscator defined for the property's generic element/value type when creating the module.
3. The obfuscator defined in the class annotations of the property's generic element/value type.
4. The global default obfuscator.

## Default character representation providers

Like default obfuscators, it's also possible to define default character representation providers per type:

    Module module = ObfuscationModule.builder()
            .withDefaultCharacterRepresentation(Date.class, d -> formatDate(d))
            .build();
    mapper.registerModule(module);

The matching will be the same as for default obfuscators.

Like type-specific obfuscators, a type-specific character representation provider will also be used for any `List`, `Set`, `Collection` or `Map` property with a matching generic element/value type that is obfuscated during deserialization.

### Character representation provider lookup order

The following order is used to look up character representation providers for properties:

1. The character representation provider defined in the property's own annotations.
2. The default character representation provider defined for the property's generic element/value type when creating the module.
3. The character representation provider defined in the class annotations of the  property's generic element/value type.
4. The result of calling [CharacterRepresentationProvider.getDefaultInstance](https://robtimus.github.io/obfuscation-annotations/apidocs/com/github/robtimus/obfuscation/annotation/CharacterRepresentationProvider.html#getDefaultInstance-java.lang.Class-) for the property's generic type. This has special support for arrays, so it's not necessary to use `@RepresentedBy` on any array property just to prevent obfuscating values like `[I@490d6c15` instead of `[1, 2, 3]`.

## Examples

### Obfuscate with a fixed length

    @ObfuscateFixedLength(3)
    private Obfuscated<String> stringValue;

### Obfuscate with the default obfuscator, but provide custom serialization of the obfuscated value

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @JsonSerialize(using = DateSerializer.class)
    @JsonDeserialize(using = DateDeserializer.class)
    @RepresentedBy(DateFormat.class)
    private Obfuscated<Date> dateValue;

Assume that DateFormat formats Date objects as `yyyy-MM-dd`, then this will obfuscate values like `1970-01-01` and not `Thu Jan 01 00:00:00 GMT 1970`.

Note that the format, serializer and deserializer target `Date`, not `Obfuscated`.

### Obfuscate a List

    @ObfuscateFixedLength(3)
    private List<String> obfuscatedList;

Note that the annotation is needed; the following is not obfuscated (unless there is a default obfuscator for `String` or one of its super types):

    private List<String> regularList;

### Obfuscate a List using a custom character representation

    @ObfuscatePortion(keepAtStart = 8)
    @RepresentedBy(DateFormat.class)
    private List<Date> obfuscatedList;

Assume that DateFormat formats Date objects as `yyyy-MM-dd`, then this will obfuscate the days, leaving values like `1970-01-**`.
