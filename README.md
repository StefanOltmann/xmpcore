# XMP Core for Kotlin Multiplatform

[![Kotlin](https://img.shields.io/badge/kotlin-2.4.20-blue.svg?logo=kotlin)](https://kotlinlang.org)
![JVM](https://img.shields.io/badge/-JVM-gray.svg?style=flat)
![Android](https://img.shields.io/badge/-Android-gray.svg?style=flat)
![iOS](https://img.shields.io/badge/-iOS-gray.svg?style=flat)
![Windows](https://img.shields.io/badge/-Windows-gray.svg?style=flat)
![Linux](https://img.shields.io/badge/-Linux-gray.svg?style=flat)
![macOS](https://img.shields.io/badge/-macOS-gray.svg?style=flat)
![JS](https://img.shields.io/badge/-JS-gray.svg?style=flat)
![WASM](https://img.shields.io/badge/-WASM-gray.svg?style=flat)

This library is a port of Adobe's XMP SDK to Kotlin Multiplatform.

## Installation

```
implementation("de.stefan-oltmann:xmpcore:2.0.0")
```

### Migration to 2.0.0

Version 2.0.0 contains a few breaking changes:

* **Dates.** `getDateTimeOriginal()` returns an `XmpDate` and `setDateTimeOriginal()` takes an
  `XmpDate`, so values keep their seconds and fractions and no longer need manual parsing and
  formatting.
* **Face regions.** `getFaces()`/`setFaces()` are replaced by `getFaceRegions()`/`setFaceRegions()`
  working on a `List<XmpFaceRegion>`, because the region list of the XMP can hold several regions
  with the same name.
* **`SerializeOptions` is immutable.** The setters return modified copies instead of mutating the
  receiver, so one shared instance can be reused from concurrent writers.
* **`XMPUtils` batch operations** `removeProperties`, `appendProperties`, `separateArrayItems` and
  `catenateArrayItems` are now available.

## How to use

The library has been designed as a drop-in replacement for users who previously used XMP Core Java.
Therefore, all the documentation applicable to the Java SDK also pertains to this library. However,
please note that we have made the decision to remove the functionality for reading from and writing
to ByteArray and InputStreams, as I believe it is unnecessary.

### Sample code

```
val originalXmp: String = "... your XMP ..."

val xmpMeta: XMPMeta = XMPMetaFactory.parseFromString(originalXmp)

val xmpSerializeOptions =
    SerializeOptions()
        .setOmitXmpMetaElement(false)
        .setOmitPacketWrapper(false)
        .setUseCompactFormat(true)
        .setSort(true)

val newXmp = XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptions)
```

Check out the [Kotlin JVM example project](examples/xmpcore-kotlin-jvm-sample).

For usage in Java projects check out the [Java example project](examples/xmpcore-java-sample).

Also see the unit tests `ReadXmpTest` and `WriteXmpTest` to learn more about reading and
manipulating data.

### Migration hint

If you have previously used the official XMP Core Java library available on Maven Central, please
make sure to update your imports from `com.adobe.internal.xmp`
to `de.stefan_oltmann.xmp`.

### Memory note

Namespaces discovered while parsing are registered permanently in a process-global schema registry
(like Adobe's XMP Core). Parsing a file with unknown namespaces therefore leaves small permanent
entries behind. Applications that parse very large numbers of files with many changing namespaces
over long uptime should keep this in mind.

### Deviations from the Adobe XMP Core

This port aims to behave like the Adobe original. A few deliberate deviations remain:

* **Fail-fast instead of swallowing errors.** The Adobe original ignores errors inside all
  `delete*` methods and returns `false` from the `doesPropertyExist*` methods when the arguments
  are invalid or a namespace is unknown. This port throws `XMPException` in those cases, because
  treating invalid input as a no-op masks programming errors.
* **No RDF found.** The Adobe original returns an empty metadata object when a document contains
  no RDF at all, which silently turns `REQUIRE_XMP_META` into a filter. This port throws
  `XMPException` instead, so callers keep control over the fallback; use `parseOrCreate` for the
  "nothing there" case.
* **Dates.** `XMPDateTime` is replaced by `XmpDate`, which parses strictly without silently
  clamping values and always renders the seconds of a time.
* **Streams.** Only `String` input and output exists. The `ByteArray`/`InputStream`/
  `OutputStream` API of the original, including the `exactPacketLength` and thumbnail padding
  options, is not ported.
* **Deterministic namespaces.** Namespace prefixes discovered while parsing are assigned in
  namespace URI order, so the serialized output does not depend on the platform's DOM attribute
  order.
* **Invariant number rendering.** `Double` values are serialized with the JVM's shortest
  round-trip spelling on every platform - plain `Double.toString` writes "0.0005" on JS and
  Wasm where the JVM writes "5.0E-4", so written files would otherwise depend on the platform
  that ran the write. An integral `Double` passed through the untyped `setProperty` cannot be
  told apart from an `Int` on JS and Wasm and keeps the integer spelling; fractional `Float`
  values keep platform-dependent spelling there as well - pass strings for byte-stable
  output.

Beyond these, the port fixes defects that the Java 5.1.3 original carries and that behave like
the Adobe C++ original here:

* A qualifier selector like `ns:bag[?ns:qual='value']` never examined the last array item in
  the Java original; this port finds it.
* A path lookup whose array index is out of range crashed the Java original with a
  `NullPointerException`; this port reports an `XMPException`.
* `XMPIterator.skipSubtree()` was declared but never evaluated by the Java original; this port
  implements the documented behavior.
* The `XMPUtils` batch operations had several defects in the Java original, among them an
  inverted form comparison that disabled array merging, array items appended to the schema
  instead of the array, and a quote scan reading the wrong position; this port implements the
  intended semantics.

A few edge cases are handled differently on purpose:

* **Duplicated properties and qualifiers.** Instead of rejecting the whole file, the last
  occurrence replaces the earlier one at its position, like ExifTool does for duplicated tags.
* **Corrupted XML around the RDF.** Junk or NUL padding around an otherwise intact RDF part is
  tolerated, where the Adobe original rejects such files.
* **Malformed RDF edge cases.** Named children inside arrays and a lone `rdf:_` element are
  rejected, while the numbered `rdf:_N` item form is accepted, following the RDF specification
  more closely than the Java original.
* **Base64 values.** Values with an invalid length or non-zero padding bits fail the read where
  the Java original silently accepted or truncated them.
* **Control characters.** Literal invalid control characters and numeric character references
  resolving to one are repaired to spaces, like Adobe's default-on repair option including its
  second parse pass.

Where a drop-in replacement is affected, the remaining API-shape differences:

* The iterator's `getNamespace()` falls back to the base namespace for array items, where the
  Adobe original returns null. An array item belongs to its schema's namespace even though its
  node name is the prefix-less `[]`, so the null of the original is an artifact of looking up
  that synthetic name as a prefix; the port reports the namespace the item was found under
  instead.

## Contributions

Contributions to this project are welcome! If you encounter any issues, have suggestions for
improvements, or would like to contribute new features, please feel free to submit a pull request.

## Acknowledgements

* JetBrains for making [Kotlin](https://kotlinlang.org).
* Adobe for making the XMP Core Java SDK.
* Paul de Vrieze for making [XmlUtil](https://github.com/pdvrieze/xmlutil).

## License

The same [BSD license](LICENSE) applies to this project as to Adobe's open source XMP SDK, from
which it is derived. See [NOTICE.md](NOTICE.md) for attributions of the original work and the
bundled third-party libraries.

Note: The original license page went offline, but you can still find it on
[archive.org](https://web.archive.org/web/20210616112605/https://www.adobe.com/devnet/xmp/library/eula-xmp-library-java.html).
