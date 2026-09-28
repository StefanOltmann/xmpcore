/*
 * =================================================================================================
 * ADOBE SYSTEMS INCORPORATED
 * Copyright 2006 Adobe Systems Incorporated
 * All Rights Reserved
 *
 * NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
 * of the Adobe license agreement accompanying it.
 * =================================================================================================
 */
package de.stefan_oltmann.xmp.internal

import de.stefan_oltmann.xmp.XMPConst
import de.stefan_oltmann.xmp.XMPException
import de.stefan_oltmann.xmp.XMPMeta
import de.stefan_oltmann.xmp.XMPSchemaRegistry
import de.stefan_oltmann.xmp.XmpDate
import de.stefan_oltmann.xmp.internal.Utils.checkUUIDFormat
import de.stefan_oltmann.xmp.internal.XMPPathParser.expandXPath
import de.stefan_oltmann.xmp.options.ParseOptions
import de.stefan_oltmann.xmp.options.PropertyOptions

internal object XMPNormalizer {

    /**
     * The prefix of an instance ID stored in the rdf:about attribute.
     */
    private const val UUID_PREFIX = "uuid:"

    /**
     * caches the correct dc-property array forms.
     */
    private val dcArrayForms = createDCArrays()

    /**
     * Normalizes a raw parsed XMPMeta-Object.
     *
     * @param xmp     the raw metadata object
     * @param options the parsing options
     * @return Returns the normalized metadata object
     *
     */
    @kotlin.jvm.JvmStatic
    fun normalize(xmp: XMPMeta, options: ParseOptions): XMPMeta {

        val tree = xmp.root

        touchUpDataModel(xmp)
        moveExplicitAliases(tree, options)
        tweakOldXMP(tree)
        deleteEmptySchemas(tree)

        return xmp
    }

    /**
     * Tweak old XMP: Move an instance ID from rdf:about to the
     * *xmpMM:InstanceID* property. An old instance ID usually looks
     * like &quot;uuid:bac965c4-9d87-11d9-9a30-000d936b79c4&quot;, plus InDesign
     * 3.0 wrote them like &quot;bac965c4-9d87-11d9-9a30-000d936b79c4&quot;.
     *
     * If the name looks like a UUID simply move it to *xmpMM:InstanceID*,
     * don't worry about any existing *xmpMM:InstanceID*. Both will
     * only be present when a newer file with the *xmpMM:InstanceID*
     * property is updated by an old app that uses *rdf:about*.
     */
    private fun tweakOldXMP(tree: XMPNode) {

        val treeName = tree.name ?: return

        if (treeName.length >= Utils.UUID_LENGTH) {

            var nameStr = treeName.lowercase()

            if (nameStr.startsWith(UUID_PREFIX))
                nameStr = nameStr.removePrefix(UUID_PREFIX)

            if (checkUUIDFormat(nameStr)) {

                /* Move UUID to xmpMM:InstanceID and remove it from the root node */
                val path = expandXPath(XMPConst.NS_XMP_MM, "InstanceID")
                val idNode = XMPNodeUtils.findNode(tree, path, true, null)

                if (idNode == null)
                    throw XMPException("Failure creating xmpMM:InstanceID", XMPErrorConst.INTERNALFAILURE)

                /* Clobber any existing xmpMM:InstanceID */
                idNode.options = PropertyOptions()
                idNode.value = "$UUID_PREFIX$nameStr"
                idNode.removeChildren()
                idNode.removeQualifiers()

                tree.name = null
            }
        }
    }

    /**
     * Visit all schemas to do general fixes and handle special cases.
     */
    private fun touchUpDataModel(xmp: XMPMeta) {

        /*
         * Make sure the DC schema is existing, because it might be needed within the normalization
         * if not touched it will be removed by removeEmptySchemas
         */
        XMPNodeUtils.findSchemaNode(xmp.root, XMPConst.NS_DC, true)

        /* Do the special case fixes within each schema. */
        val it = xmp.root.iterateChildren()

        while (it.hasNext()) {

            val currSchema = it.next()

            when {

                XMPConst.NS_DC == currSchema.name ->
                    normalizeDCArrays(currSchema)

                XMPConst.NS_EXIF == currSchema.name -> {

                    /* Do a special case fix for exif:GPSTimeStamp. */
                    fixGpsTimeStamp(currSchema)

                    XMPNodeUtils.findChildNode(currSchema, "exif:UserComment", false)
                        ?.let { userComment -> repairAltText(userComment) }
                }

                XMPConst.NS_DM == currSchema.name -> {

                    /*
                     * Do a special case migration of xmpDM:copyright to
                     * dc:rights['x-default'].
                     */
                    XMPNodeUtils.findChildNode(currSchema, "xmpDM:copyright", false)
                        ?.let { dmCopyright -> migrateAudioCopyright(xmp, dmCopyright) }
                }

                XMPConst.NS_XMP_RIGHTS == currSchema.name ->
                    XMPNodeUtils.findChildNode(currSchema, "xmpRights:UsageTerms", false)
                        ?.let { usageTerms -> repairAltText(usageTerms) }
            }
        }
    }

    /**
     * Repairs a defective exif:GPSTimeStamp whose calendar part is missing entirely
     * ("0000-00-00..."), a pattern GPS trackers without a satellite fix write. The
     * calendar part is replaced with the one from exif:DateTimeOriginal, or from
     * exif:DateTimeDigitized when the original is absent. Missing or bad dates skip the
     * repair silently, so nothing else is blocked, exactly like the Adobe original.
     *
     * @param exifSchema the exif schema node
     */
    private fun fixGpsTimeStamp(exifSchema: XMPNode) {

        val gpsDateTime = XMPNodeUtils.findChildNode(exifSchema, "exif:GPSTimeStamp", false)
            ?: return

        val gpsValue = gpsDateTime.value ?: return

        /*
         * The value is inspected as text, because a date-less value like
         * "0000-00-00T10:20:30" is not a form the strict date parser accepts. Only the
         * canonical all-zero calendar part triggers the repair.
         */
        val missingCalendarPart = "0000-00-00"

        if (!gpsValue.startsWith(missingCalendarPart))
            return

        val otherDate = XMPNodeUtils.findChildNode(exifSchema, "exif:DateTimeOriginal", false)
            ?: XMPNodeUtils.findChildNode(exifSchema, "exif:DateTimeDigitized", false)
            ?: return

        val calendarPrefix = otherDate.value?.let { parseCalendarPrefix(it) } ?: return

        gpsDateTime.value = calendarPrefix + gpsValue.substring(missingCalendarPart.length)
    }

    /**
     * Renders the four-digit calendar prefix (year, optional month and day) of an XMP
     * date, or null when the value is not a well-formed date with a year.
     */
    private fun parseCalendarPrefix(value: String): String? {

        val date = XmpDate.parse(value) ?: return null

        if (date.year == 0)
            return null

        val builder = StringBuilder(date.year.toString().padStart(4, '0'))

        if (date.month > 0)
            builder.append('-').append(date.month.toString().padStart(2, '0'))

        if (date.day > 0)
            builder.append('-').append(date.day.toString().padStart(2, '0'))

        return builder.toString()
    }

    /**
     * Migrates the audio copyright of xmpDM:copyright into dc:rights['x-default'] and
     * removes the source property afterwards, following the Adobe original: the existing
     * x-default tail behind a double linefeed is replaced, an identical tail is kept, and
     * a missing or bad dc:rights form stops the migration without blocking other cleanup.
     *
     * @param xmp         the metadata object
     * @param dmCopyright the xmpDM:copyright property node
     */
    private fun migrateAudioCopyright(
        xmp: XMPMeta,
        dmCopyright: XMPNode
    ) {

        try {

            val dcSchema = XMPNodeUtils.findSchemaNode(xmp.root, XMPConst.NS_DC, true)
                ?: return

            val dmValue = dmCopyright.value ?: return

            val doubleLF = "\n\n"

            val dcRightsArray = XMPNodeUtils.findChildNode(dcSchema, "dc:rights", false)

            if (dcRightsArray == null || !dcRightsArray.hasChildren()) {

                /* 1. No dc:rights array: create from double linefeed and xmpDM:copyright. */
                xmp.setLocalizedText(
                    XMPConst.NS_DC, "rights", "", XMPConst.X_DEFAULT, doubleLF + dmValue
                )
            } else {

                var xdIndex = XMPNodeUtils.lookupLanguageItem(dcRightsArray, XMPConst.X_DEFAULT)

                if (xdIndex < 0) {

                    /* 2. No x-default item: create from the first item. */
                    val firstValue = dcRightsArray.getChild(1).value ?: ""

                    xmp.setLocalizedText(XMPConst.NS_DC, "rights", "", XMPConst.X_DEFAULT, firstValue)

                    xdIndex = XMPNodeUtils.lookupLanguageItem(dcRightsArray, XMPConst.X_DEFAULT)
                }

                if (xdIndex < 0)
                    return

                /* 3. Look for a double linefeed in the x-default value. */
                val defaultNode = dcRightsArray.getChild(xdIndex)
                val defaultValue = defaultNode.value ?: ""
                val lfPos = defaultValue.indexOf(doubleLF)

                if (lfPos < 0) {

                    /* 3A. No double linefeed: compare whole values. */
                    if (dmValue != defaultValue) {

                        /* 3A2. Append the xmpDM:copyright to the x-default item. */
                        defaultNode.value = defaultValue + doubleLF + dmValue
                    }
                } else {

                    /* 3B. Has double linefeed: compare the tail. */
                    if (defaultValue.substring(lfPos + 2) != dmValue) {

                        /* 3B2. Replace the x-default tail. */
                        defaultNode.value = defaultValue.substring(0, lfPos + 2) + dmValue
                    }
                }
            }

            /* 4. Get rid of the xmpDM:copyright. */
            dmCopyright.parent?.removeChild(dmCopyright)

        } catch (_: XMPException) {

            /* Don't let failures (like a bad dc:rights form) stop other cleanup. */
        }
    }

    /**
     * Undo the denormalization performed by the XMP used in Acrobat 5.
     * If a Dublin Core array had only one item, it was serialized as a simple property.
     * The `xml:lang` attribute was dropped from an `alt-text` item if the language was `x-default`.
     *
     */    private fun normalizeDCArrays(dcSchema: XMPNode) {

        for (index in 1..dcSchema.getChildrenLength()) {

            val currProp = dcSchema.getChild(index)

            val arrayForm = dcArrayForms[currProp.name]

            if (arrayForm == null) {

                continue

            } else if (currProp.options.isSimple()) {

                /*
                 * Create a new array and add the current property as child, if it was formerly simple.
                 * The form template is copied: handing the shared instance to a live node would let
                 * any later option mutation (clients can reach node options through getProperty())
                 * corrupt the process-global templates and silently change all future normalizations.
                 */
                val newArray = XMPNode(
                    name = currProp.name,
                    value = null,
                    options = PropertyOptions(arrayForm.getOptions())
                )

                currProp.name = XMPConst.ARRAY_ITEM_NAME
                newArray.addChild(currProp)
                dcSchema.replaceChild(index, newArray)

                /* Fix language alternatives */
                if (arrayForm.isArrayAltText() && !currProp.options.hasLanguage()) {

                    val newLang = XMPNode(XMPConst.XML_LANG, XMPConst.X_DEFAULT)

                    currProp.addQualifier(newLang)
                }

            } else {

                /* Clear array options and add corrected array form if it has been an array before */
                currProp.options.setOption(
                    PropertyOptions.ARRAY or
                        PropertyOptions.ARRAY_ORDERED or
                        PropertyOptions.ARRAY_ALTERNATE or
                        PropertyOptions.ARRAY_ALT_TEXT,
                    false
                )

                currProp.options.mergeWith(arrayForm)

                /* Applying for "dc:description", "dc:rights", "dc:title" */
                if (arrayForm.isArrayAltText())
                    repairAltText(currProp)
            }
        }
    }

    /**
     * Make sure that the array is well-formed AltText. Each item must be simple
     * and have an "xml:lang" qualifier. If repairs are needed, keep simple
     * non-empty items by adding the "xml:lang" with value "x-repair".
     *
     * @param arrayNode the property node of the array to repair.
     */
    private fun repairAltText(arrayNode: XMPNode?) {

        /* Already OK or not even an array. */
        if (arrayNode == null || !arrayNode.options.isArray())
            return

        /* Fix options */
        arrayNode.options.setArrayOrdered(true).setArrayAlternate(true).setArrayAltText(true)

        val it = arrayNode.iterateChildrenMutable()

        while (it.hasNext()) {

            val currChild = it.next()

            if (currChild.options.isCompositeProperty()) {

                /* Delete non-simple children. */
                it.remove()

            } else if (!currChild.options.hasLanguage()) {

                val childValue = currChild.value

                if (childValue.isNullOrEmpty()) {

                    /* Delete empty valued children that have no xml:lang. */
                    it.remove()

                } else {

                    /* Add a xml:lang qualifier with the value "x-repair". */
                    val repairLang = XMPNode(XMPConst.XML_LANG, "x-repair")
                    currChild.addQualifier(repairLang)
                }
            }
        }
    }

    /**
     * Visit all the top level nodes looking for aliases. If there is
     * no base, transplant the alias subtree. If there is a base and strict
     * aliasing is on, make sure the alias and base subtrees match.
     *
     * @param tree    the root of the metadata tree
     * @param options th parsing options
     */
    private fun moveExplicitAliases(tree: XMPNode, options: ParseOptions) {

        if (!tree.hasAliases)
            return

        tree.hasAliases = false

        val strictAliasing = options.getStrictAliasing()

        val schemas = tree.getChildren().toList()

        for (currSchema in schemas) {

            if (!currSchema.hasAliases)
                continue

            val properties = currSchema.getChildren().toList()

            for (currProp in properties) {

                if (!currProp.isAlias)
                    continue

                currProp.isAlias = false

                moveExplicitAlias(tree, currSchema, currProp, strictAliasing)
            }

            currSchema.hasAliases = false
        }
    }

    /**
     * Moves an explicit alias property to its base property or, if the base
     * node exists, checks the alias subtree against the base subtree when
     * strict aliasing is on.
     *
     * @param tree           the root of the metadata tree
     * @param currSchema     the schema containing the alias property
     * @param currProp       the alias property to move or check
     * @param strictAliasing true if the alias and base subtrees shall match
     */
    private fun moveExplicitAlias(
        tree: XMPNode,
        currSchema: XMPNode,
        currProp: XMPNode,
        strictAliasing: Boolean
    ) {

        /* Find the base path, look for the base schema and root node. */
        val info = XMPSchemaRegistry.findAlias(requireNotNull(currProp.name))

        if (info != null) {

            /* Find or create schema */
            val baseSchema = XMPNodeUtils.findSchemaNode(
                tree, info.getNamespace(), null, true
            )

            checkNotNull(baseSchema) { "SchemaNode should have been created." }

            baseSchema.isImplicit = false

            var baseNode = XMPNodeUtils.findChildNode(
                baseSchema,
                info.getPrefix() + info.getPropName(), false
            )

            if (baseNode == null) {

                if (info.getAliasForm().isSimple()) {

                    /*
                     * A top-to-top alias, transplant the property.
                     * change the alias property name to the base name
                     */
                    val qname = info.getPrefix() + info.getPropName()

                    currProp.name = qname

                    baseSchema.addChild(currProp)

                    /* Remove the alias property */

                    currSchema.removeChild(currProp)

                } else {

                    /*
                     * An alias to an array item,
                     * create the array and transplant the property.
                     */
                    baseNode = XMPNode(
                        name = info.getPrefix() + info.getPropName(),
                        value = null,
                        options = info.getAliasForm().toPropertyOptions()
                    )

                    baseSchema.addChild(baseNode)

                    transplantArrayItemAlias(currProp, baseNode) {
                        currSchema.removeChild(currProp)
                    }
                }

            } else if (info.getAliasForm().isSimple()) {

                /*
                 * The base node does exist and this is a top-to-top alias.
                 * Check for conflicts if strict aliasing is on.
                 * Remove and delete the alias subtree.
                 */
                if (strictAliasing)
                    compareAliasedSubtrees(currProp, baseNode, true)

                currSchema.removeChild(currProp)

            } else {

                /*
                 * This is an alias to an array item and the array exists.
                 * Look for the aliased item.
                 * Then transplant or check & delete as appropriate.
                 */
                var itemNode: XMPNode? = null

                if (info.getAliasForm().isArrayAltText()) {

                    val xdIndex = XMPNodeUtils.lookupLanguageItem(baseNode, XMPConst.X_DEFAULT)

                    if (xdIndex != -1)
                        itemNode = baseNode.getChild(xdIndex)

                } else if (baseNode.hasChildren()) {

                    itemNode = baseNode.getChild(1)
                }

                if (itemNode == null) {

                    transplantArrayItemAlias(currProp, baseNode) {
                        currSchema.removeChild(currProp)
                    }

                } else {

                    if (strictAliasing)
                        compareAliasedSubtrees(currProp, itemNode, true)

                    currSchema.removeChild(currProp)
                }
            }
        }
    }

    /**
     * Moves an alias node of array form to another schema into an array.
     *
     * @param childNode  the node to be moved
     * @param baseArray  the base array for the array item
     * @param removeChildFromTree lambda used to delete the property of the schema
     */
    private fun transplantArrayItemAlias(
        childNode: XMPNode,
        baseArray: XMPNode,
        removeChildFromTree: () -> Unit
    ) {

        if (baseArray.options.isArrayAltText()) {

            /* *** Allow x-default. */
            if (childNode.options.hasLanguage())
                throw XMPException(
                    "Alias to x-default already has a language qualifier",
                    XMPErrorConst.BADXMP
                )

            val langQual = XMPNode(XMPConst.XML_LANG, XMPConst.X_DEFAULT)

            childNode.addQualifier(langQual)
        }

        removeChildFromTree()

        childNode.name = XMPConst.ARRAY_ITEM_NAME

        baseArray.addChild(childNode)
    }

    /**
     * Remove all empty schemas from the metadata tree that were generated during the rdf parsing.
     *
     * @param tree the root of the metadata tree
     */
    private fun deleteEmptySchemas(tree: XMPNode) {

        /* Delete empty schema nodes. Do this last, other cleanup can make empty schema. */

        val it = tree.iterateChildrenMutable()

        while (it.hasNext()) {

            val schema = it.next()

            if (!schema.hasChildren())
                it.remove()
        }
    }

    /**
     * The outermost call is special. The names almost certainly differ. The
     * qualifiers (and hence options) will differ for an alias to the x-default
     * item of a langAlt array.
     *
     * @param aliasNode the alias node
     * @param baseNode  the base node of the alias
     * @param outerCall marks the outer call of the recursion
     *
     */
    private fun compareAliasedSubtrees(
        aliasNode: XMPNode,
        baseNode: XMPNode,
        outerCall: Boolean
    ) {

        if (aliasNode.value != baseNode.value || aliasNode.getChildrenLength() != baseNode.getChildrenLength())
            throw XMPException("Mismatch between alias and base nodes", XMPErrorConst.BADXMP)

        val nodeMismatch = aliasNode.name != baseNode.name ||
            aliasNode.options != baseNode.options ||
            aliasNode.getQualifierLength() != baseNode.getQualifierLength()

        if (!outerCall && nodeMismatch)
            throw XMPException("Mismatch between alias and base nodes", XMPErrorConst.BADXMP)

        run {
            val an = aliasNode.iterateChildren()
            val bn = baseNode.iterateChildren()

            while (an.hasNext() && bn.hasNext()) {
                val aliasChild = an.next()
                val baseChild = bn.next()
                compareAliasedSubtrees(aliasChild, baseChild, false)
            }
        }

        val an = aliasNode.iterateQualifier()
        val bn = baseNode.iterateQualifier()

        while (an.hasNext() && bn.hasNext()) {

            val aliasQual = an.next()
            val baseQual = bn.next()

            compareAliasedSubtrees(aliasQual, baseQual, false)
        }
    }

    /**
     * Initializes the map that contains the known arrays, that are fixed by
     * [XMPNormalizer.normalizeDCArrays].
     */
    private fun createDCArrays(): Map<String, PropertyOptions> {

        val dcArrayForms = mutableMapOf<String, PropertyOptions>()

        /* Properties supposed to be a "Bag". */
        val bagForm = PropertyOptions()
        bagForm.setArray(true)
        dcArrayForms["dc:contributor"] = bagForm
        dcArrayForms["dc:language"] = bagForm
        dcArrayForms["dc:publisher"] = bagForm
        dcArrayForms["dc:relation"] = bagForm
        dcArrayForms["dc:subject"] = bagForm
        dcArrayForms["dc:type"] = bagForm

        /* Properties supposed to be a "Seq". */
        val seqForm = PropertyOptions()
        seqForm.setArray(true)
        seqForm.setArrayOrdered(true)
        dcArrayForms["dc:creator"] = seqForm
        dcArrayForms["dc:date"] = seqForm

        /* Properties supposed to be an "Alt" in alternative-text form. */
        val altTextForm = PropertyOptions()
        altTextForm.setArray(true)
        altTextForm.setArrayOrdered(true)
        altTextForm.setArrayAlternate(true)
        altTextForm.setArrayAltText(true)
        dcArrayForms["dc:description"] = altTextForm
        dcArrayForms["dc:rights"] = altTextForm
        dcArrayForms["dc:title"] = altTextForm

        return dcArrayForms
    }
}
