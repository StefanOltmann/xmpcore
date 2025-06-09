# XMP Core JavaScript Sample

This example demonstrates how to use the XMP Core library from a pure JavaScript project.

## Prerequisites

Before running this example, you need to build the XMP Core library for JavaScript:

```bash
# From the root of the xmpcore project
./gradlew jsBrowserProductionWebpack
```

This will generate the `xmpcore.js` file in the `build/kotlin-webpack/js/xmpcore/` directory.

## Running the Example

1. Build the XMP Core library as described above
2. Open `index.html` in a web browser

## How to Use XMP Core in Your JavaScript Project

### 1. Include the Library

Include the XMP Core library in your HTML file:

```html

<script src="path/to/build/kotlin-webpack/js/xmpcore/xmpcore.js"></script>
```

### 2. Access the Library

The library is accessible through the `xmpcore` object:

```javascript
// Create serialize options
const serializeOptions = new xmpcore.com.ashampoo.xmp.options.SerializeOptions()
    .setOmitXmpMetaElement(false)
    .setOmitPacketWrapper(false)
    .setUseCompactFormat(true)
    .setSort(true);

// Create a new XMP metadata object
const xmpMeta = xmpcore.com.ashampoo.xmp.XMPMetaFactory.create();

// Set properties
xmpMeta.setPropertyInteger(
    xmpcore.com.ashampoo.xmp.XMPConst.NS_XMP,
    "Rating",
    3
);

// Serialize to XMP string
const xmpString = xmpcore.com.ashampoo.xmp.XMPMetaFactory.serializeToString(
    xmpMeta,
    serializeOptions
);
```

### 3. Parse Existing XMP

```javascript
// Create parse options
const parseOptions = new xmpcore.com.ashampoo.xmp.options.ParseOptions()
    .setRequireXMPMeta(false);

// Parse XMP string
const xmpMeta = xmpcore.com.ashampoo.xmp.XMPMetaFactory.parseFromString(xmpString, parseOptions);

// Read properties
const rating = xmpMeta.getPropertyInteger(xmpcore.com.ashampoo.xmp.XMPConst.NS_XMP, "Rating");
const keywords = xmpMeta.getKeywords();
```

## Notes

- The library is exported as the global `xmpcore` object, and the Kotlin package structure is preserved under this object (e.g., `xmpcore.com.ashampoo.xmp`).
- You can use `console.log(xmpcore)` to inspect the available classes and methods.
- Error handling is important when working with XMP data, as shown in the example code.
