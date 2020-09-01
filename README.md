# obfuscation-jackson-databind

Provides support for serializing and deserializing obfuscated values using Jackson. All you need to do is register a module:

    Module module = ObfuscationModule.defaultModule();
    mapper.registerModule(module);

This will automatically allow all instances of [Obfuscated](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscated.html) to be serialized and deserialized, without the need for any custom serializer or deserializer. In fact, any annotation used (apart from the ones below) will be used for the `Obfuscated` object's *value*, not the `Obfuscated` object itself. That means that you can provide custom serialization and/or deserialization for the value the way you're used to, without needing to wrap it inside a new annotation.

By default, deserialized `Obfuscated` objects will use [Obfuscator.fixedLength(3)](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscator.html#fixedLength-int-). To override this default, just use a builder to create the module instead:

    Module module = ObfuscationModule.builder()
            .withDefaultObfuscator(Obfuscator.fixedValue("<obfuscated>"))
            .build();
    mapper.registerModule(module);

## Annotation-based obfuscation

With the annotations from [obfuscation-annotations](https://robtimus.github.io/obfuscation-annotations), you can control how values are obfuscated during deserialization. This is supported for the following types:

### Obfuscated

* Use an annotation like `@ObfuscateFixedLength(3)` or `@ObfuscateAll` to specify a custom obfuscator to use for the `Obfuscated` property.
* Use `@RepresentedBy` to provide a custom string representation.

### List, Set, Collection and Map

If properties declared as `List`, `Set`, `Collection` and `Map` are annotated with an annotation like `@ObfuscateFixedLength(3)` or `@ObfuscateAll`, they will be obfuscated using the specified obfuscator during deserialization. This is done using [Obfuscator.obfuscateList](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscator.html#obfuscateList-java.util.List-java.util.function.Function-), [Obfuscator.obfuscateSet](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscator.html#obfuscateSet-java.util.Set-java.util.function.Function-), [Obfuscator.obfuscateCollection](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscator.html#obfuscateCollection-java.util.Collection-java.util.function.Function-) and [Obfuscator.obfuscateMap](https://robtimus.github.io/obfuscation-core/apidocs/com/github/robtimus/obfuscation/Obfuscator.html#obfuscateMap-java.util.Map-java.util.function.Function-) respectively. If the property is annotated with `@RepresentedBy` it will use this custom string representation; otherwise it will use `Object.toString()`.

## Examples

    // Obfuscate with a fixed length
    @ObfuscateFixedLength(3)
    private Obfuscated<String> stringValue;

    // Obfuscate with the default obfuscator, but provide custom serialization of the obfuscated value
    // Note how the serializer and deserializer target Date, not Obfuscated
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @JsonSerialize(using = DateSerializer.class)
    @JsonDeserialize(using = DateDeserializer.class)
    private Obfuscated<Date> dateValue;

    // Obfuscate with a custom string representation
    // For arrays this is necessary to prevent obfuscating values like [I@490d6c15 instead of [1, 2, 3]
    @ObfuscatePortion(keepAtStart = 1, keepAtEnd = 1, fixedLength = 3)
    @RepresentedBy(IntArrayToString.class)
    private Obfuscated<int[]> intArray;

    // Obfuscate a List
     @ObfuscateFixedLength(3)
     private List<String> obfuscatedList;
    // Note that the annotation is needed; the following is not obfuscated
    private List<String> regularList;

    // Obfuscate a List using a custom string representation
    // Assume that DateFormat formats Date objects as yyyy-MM-dd,
    // then this will obfuscate the days, leaving values like 1970-01-**
    @ObfuscatePortion(keepAtStart = 8)
    @RepresentedBy(DateFormat.class)
    private List<Date> obfuscatedList;
