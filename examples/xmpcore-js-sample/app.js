// Sample JavaScript code to demonstrate using XMP Core from JavaScript

document.addEventListener('DOMContentLoaded', function () {

    const outputDiv = document.getElementById('output');

    try {
        // Access the XMP Core library
        // Note: The actual namespace might be different depending on how the JS is compiled
        // It could be 'com.ashampoo.xmp' or just 'xmpcore'

        // Create serialize options
        const serializeOptions = new com.ashampoo.xmp.options.SerializeOptions()
            .setOmitXmpMetaElement(false)
            .setOmitPacketWrapper(false)
            .setUseCompactFormat(true)
            .setUseCanonicalFormat(false)
            .setSort(true);

        // Create parse options
        const parseOptions = new com.ashampoo.xmp.options.ParseOptions()
            .setRequireXMPMeta(false);

        // Example 1: Create new XMP metadata
        outputDiv.innerHTML += '<h2>Example 1: Creating new XMP metadata</h2>';

        // Create a new XMP metadata object
        const newXmpMeta = com.ashampoo.xmp.XMPMetaFactory.create();

        // Set a rating property
        newXmpMeta.setPropertyInteger(
            com.ashampoo.xmp.XMPConst.NS_XMP,
            "Rating",
            3
        );

        // Set keywords
        newXmpMeta.setKeywords(
            ["cat", "cute", "animal"]
        );

        // Serialize to XMP string
        const newXmp = com.ashampoo.xmp.XMPMetaFactory.serializeToString(
            newXmpMeta,
            serializeOptions
        );

        // Display the result
        outputDiv.innerHTML += '<pre>' + escapeHtml(newXmp) + '</pre>';

        // Example 2: Parse existing XMP metadata
        outputDiv.innerHTML += '<h2>Example 2: Parsing existing XMP metadata</h2>';

        // Sample XMP string
        const oldXmp =
            `<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:dc="http://purl.org/dc/elements/1.1/"
                    xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                  <xmp:Rating>5</xmp:Rating>
                  <dc:subject>
                    <rdf:Bag>
                      <rdf:li>animal</rdf:li>
                      <rdf:li>bird</rdf:li>
                    </rdf:Bag>
                  </dc:subject>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>`;

        // Parse the XMP string
        const oldXmpMeta = com.ashampoo.xmp.XMPMetaFactory.parseFromString(oldXmp, parseOptions);

        // Get properties from the parsed XMP
        const rating = oldXmpMeta.getPropertyInteger(com.ashampoo.xmp.XMPConst.NS_XMP, "Rating");
        const keywords = oldXmpMeta.getKeywords();

        // Display the results
        outputDiv.innerHTML += '<p>Rating: ' + rating + '</p>';
        outputDiv.innerHTML += '<p>Keywords: ' + keywords.join(', ') + '</p>';

    } catch (error) {
        outputDiv.innerHTML += '<div class="error">Error: ' + error.message + '</div>';
        console.error(error);
    }
});

// Helper function to escape HTML for display
function escapeHtml(unsafe) {
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
