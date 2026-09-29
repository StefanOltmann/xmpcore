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
package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.XMPUtils.itemValuesMatch
import de.stefan_oltmann.xmp.internal.Utils.isInternalProperty
import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.internal.XMPNode
import de.stefan_oltmann.xmp.internal.XMPNodeUtils.findChildNode
import de.stefan_oltmann.xmp.internal.XMPNodeUtils.findNode
import de.stefan_oltmann.xmp.internal.XMPNodeUtils.findSchemaNode
import de.stefan_oltmann.xmp.internal.XMPNodeUtils.lookupLanguageItem
import de.stefan_oltmann.xmp.internal.XMPNodeUtils.verifySetOptions
import de.stefan_oltmann.xmp.internal.XMPPath
import de.stefan_oltmann.xmp.internal.XMPPathParser.expandXPath
import de.stefan_oltmann.xmp.options.PropertyOptions

/**
 * Utility methods to process whole metadata trees, ported from the Adobe XMP Core.
 */
public object XMPUtils {

    /* Character kind constants for splitting and joining array items. */
    private const val UCK_NORMAL = 0
    private const val UCK_SPACE = 1
    private const val UCK_COMMA = 2
    private const val UCK_SEMICOLON = 3
    private const val UCK_QUOTE = 4
    private const val UCK_CONTROL = 5

    /** ASCII, ideographic and additional space characters. */
    private const val SPACES = " \u3000\u303F"

    /** ASCII, full width and additional comma characters. */
    private const val COMMAS = ",\uFF0C\uFF64\uFE50\uFE51\u3001\u060C\u055D"

    /** ASCII, full width and additional semicolon characters. */
    private const val SEMICOLA = ";\uFF1B\uFE54\u061B\u037E"

    /** ASCII and typographic quote characters, see also the range checks. */
    private const val QUOTES = "\"\u00AB\u00BB\u301D\u301E\u301F\u2015\u2039\u203A"

    /** Additional line and paragraph separators counted as control characters. */
    private const val CONTROLS = "\u2028\u2029"

    /** The Unicode range of en quad through zero width space, all counted as spaces. */
    private val SPACE_RANGE = 0x2000..0x200B

    /** The Unicode range of CJK bracket quotes. */
    private val BRACKET_QUOTE_RANGE = 0x3008..0x300F

    /** The Unicode range of typographic quotes. */
    private val TYPOGRAPHIC_QUOTE_RANGE = 0x2018..0x201F

    /** The first ASCII control character. */
    private const val FIRST_ASCII_CONTROL = 0x0020

    /**
     * Removes a property, all properties of a schema, or all properties of all schemas.
     *
     * With a property name only that one property is removed, and a schema namespace is
     * required. With only a schema namespace all properties of that schema are removed,
     * optionally including the properties the schema's aliases point to. With neither
     * argument all properties of all schemas are removed. Properties that the XMP
     * specification defines as internal (like xmp:ModifyDate) are kept unless
     * [doAllProperties] is set, and schemas that become empty are removed.
     *
     * @param xmp              the metadata object to clean up.
     * @param schemaNS         the schema namespace, or null for all schemas.
     * @param propName         the property to remove, or null for all properties.
     * @param doAllProperties  also remove properties marked as internal.
     * @param includeAliases   also remove the properties the schema's aliases point to.
     * @throws XMPException If a property name is given without a schema namespace.
     */
    public fun removeProperties(
        xmp: XMPMeta,
        schemaNS: String?,
        propName: String?,
        doAllProperties: Boolean,
        includeAliases: Boolean
    ) {

        if (!propName.isNullOrEmpty()) {

            /*
             * Remove just the one indicated property. This might be an alias, the named
             * schema might not actually exist. So don't look up the schema node.
             */

            if (schemaNS.isNullOrEmpty())
                throw XMPException("Property name requires schema namespace", XMPErrorConst.BADPARAM)

            val expPath = expandXPath(schemaNS, propName)

            val propNode = findNode(xmp.root, expPath, false, null)

            if (propNode != null &&
                (
                    doAllProperties || !isInternalProperty(
                        expPath.getSegment(XMPPath.STEP_SCHEMA).name,
                        expPath.getSegment(XMPPath.STEP_ROOT_PROP).name
                    )
                    )
            ) {

                val parent = propNode.parent

                if (parent != null) {

                    parent.removeChild(propNode)

                    if (parent.options.isSchemaNode() && !parent.hasChildren()) {

                        /* Remove empty schema node */
                        parent.parent?.removeChild(parent)
                    }
                }
            }
        } else if (!schemaNS.isNullOrEmpty()) {

            /*
             * Remove all properties from the named schema. Optionally include aliases, in
             * which case there might not be an actual schema node.
             */

            val schemaNode = findSchemaNode(xmp.root, schemaNS, false)

            if (schemaNode != null && removeSchemaChildren(schemaNode, doAllProperties)) {
                xmp.root.removeChild(schemaNode)
            }

            if (includeAliases) {

                /*
                 * Remove the properties the schema's aliases point to. Looking up the
                 * node from the alias makes sure the actual property exists.
                 */
                for (info in XMPSchemaRegistry.findAliases(schemaNS)) {

                    val path = expandXPath(info.getNamespace(), info.getPropName())

                    val actualProp = findNode(xmp.root, path, false, null)

                    actualProp?.parent?.removeChild(actualProp)
                }
            }
        } else {

            /*
             * Remove all appropriate properties from all schemas. In this case we don't
             * have to be concerned with aliases, they are handled implicitly from the
             * actual properties.
             */
            val schemas = xmp.root.iterateChildrenMutable()

            while (schemas.hasNext()) {

                val schema = schemas.next()

                if (removeSchemaChildren(schema, doAllProperties))
                    schemas.remove()
            }
        }
    }

    /**
     * Removes all removable properties of a schema.
     *
     * @param schemaNode      the schema node to clean up.
     * @param doAllProperties also remove properties marked as internal.
     * @return Returns true when the schema has no children left.
     */
    private fun removeSchemaChildren(schemaNode: XMPNode, doAllProperties: Boolean): Boolean {

        val properties = schemaNode.iterateChildrenMutable()

        while (properties.hasNext()) {

            val currProp = properties.next()

            if (doAllProperties || !isInternalProperty(schemaNode.name, currProp.name))
                properties.remove()
        }

        return !schemaNode.hasChildren()
    }

    /**
     * Appends properties from a source metadata object into a destination object,
     * leaving the source untouched.
     *
     * Properties marked as internal (like xmp:ModifyDate) are only appended when
     * [doAllProperties] is set. Existing properties are only replaced when
     * [replaceOldValues] is set, otherwise simple properties are kept and structs and
     * arrays are merged. With [deleteEmptyValues] a source property without value removes
     * the corresponding destination property.
     *
     * @param source            the metadata object to read from.
     * @param destination       the metadata object to write into.
     * @param doAllProperties   also append properties marked as internal.
     * @param replaceOldValues  replace existing destination properties completely.
     * @param deleteEmptyValues let empty source properties delete destination properties.
     */
    public fun appendProperties(
        source: XMPMeta,
        destination: XMPMeta,
        doAllProperties: Boolean,
        replaceOldValues: Boolean,
        deleteEmptyValues: Boolean
    ) {

        val sourceSchemas = source.root.iterateChildren()

        while (sourceSchemas.hasNext()) {

            val sourceSchema = sourceSchemas.next()

            /* Make sure we have a destination schema node */
            var destSchema = findSchemaNode(destination.root, requireNotNull(sourceSchema.name), false)

            var createdSchema = false

            if (destSchema == null) {

                destSchema = XMPNode(
                    name = sourceSchema.name,
                    value = sourceSchema.value,
                    options = PropertyOptions().setSchemaNode(true)
                )

                destination.root.addChild(destSchema)

                createdSchema = true
            }

            /* Process the source schema's children. */
            val sourceProps = sourceSchema.iterateChildren()

            while (sourceProps.hasNext()) {

                val sourceProp = sourceProps.next()

                if (doAllProperties || !isInternalProperty(sourceSchema.name, sourceProp.name))
                    appendSubtree(destination, sourceProp, destSchema, replaceOldValues, deleteEmptyValues)
            }

            if (!destSchema.hasChildren() && (createdSchema || deleteEmptyValues)) {

                /* Don't create an empty schema / remove empty schema. */
                destination.root.removeChild(destSchema)
            }
        }
    }

    /**
     * Appends one source property to the destination parent, following the Adobe rules:
     * a missing destination node is cloned over, an existing node is replaced completely
     * on request, and structs and arrays are merged field by field and item by item.
     *
     * Note: The Java 5.1.3 original has defects in the merge branches (an inverted form
     * comparison in [itemValuesMatch], array items appended to the schema instead of the
     * array, a reference instead of a value comparison that disables merging entirely and
     * a wrong clone direction for alt-text items). This port implements the intended
     * semantics of the Adobe C++ original instead.
     */
    private fun appendSubtree(
        destination: XMPMeta,
        sourceNode: XMPNode,
        destParent: XMPNode,
        replaceOldValues: Boolean,
        deleteEmptyValues: Boolean
    ) {

        val sourceName = sourceNode.name ?: return

        var destNode = findChildNode(destParent, sourceName, false)

        var valueIsEmpty = false

        if (deleteEmptyValues) {
            valueIsEmpty = if (sourceNode.options.isSimple())
                sourceNode.value.isNullOrEmpty()
            else
                !sourceNode.hasChildren()
        }

        if (deleteEmptyValues && valueIsEmpty) {

            if (destNode != null)
                destParent.removeChild(destNode)

        } else if (destNode == null) {

            /* The one easy case: the destination does not exist. */
            destParent.addChild(sourceNode.clone())

        } else if (replaceOldValues) {

            /* The destination exists and should be replaced. */
            destination.setNode(destNode, sourceNode.value, sourceNode.options, true)

            destParent.removeChild(destNode)

            destParent.addChild(sourceNode.clone())

        } else {

            /* The destination exists and is not totally replaced. Structs and arrays are merged. */

            val sourceForm = sourceNode.options
            val destForm = destNode.options

            if (sourceForm.getOptions() != destForm.getOptions())
                return

            if (sourceForm.isStruct())
                mergeStructFields(destination, sourceNode, destNode, destParent, deleteEmptyValues)
            else if (sourceForm.isArrayAltText())
                mergeAltTextArray(destNode, destParent, sourceNode, deleteEmptyValues)
            else if (sourceForm.isArray())
                mergeArrayByItems(destNode, sourceNode)
        }
    }

    /**
     * Merges the fields of a source struct into the destination struct field by field.
     *
     * @param destination      the metadata object to write into.
     * @param sourceNode       the source struct.
     * @param destNode         the destination struct.
     * @param destParent       the parent of the destination struct.
     * @param deleteEmptyValues let empty source fields delete destination fields.
     */
    private fun mergeStructFields(
        destination: XMPMeta,
        sourceNode: XMPNode,
        destNode: XMPNode,
        destParent: XMPNode,
        deleteEmptyValues: Boolean
    ) {

        /* To merge a struct process the fields recursively. */

        val sourceFields = sourceNode.iterateChildren()

        while (sourceFields.hasNext()) {

            val sourceField = sourceFields.next()

            appendSubtree(destination, sourceField, destNode, false, deleteEmptyValues)

            if (deleteEmptyValues && !destNode.hasChildren())
                destParent.removeChild(destNode)
        }
    }

    /**
     * Merges an alt-text array by the "xml:lang" qualifiers, keeping x-default first.
     * Empty source items delete the language item in the destination.
     *
     * @param destNode          the destination array.
     * @param destParent        the parent of the destination array.
     * @param sourceNode        the source array.
     * @param deleteEmptyValues let empty source items delete destination items.
     */
    private fun mergeAltTextArray(
        destNode: XMPNode,
        destParent: XMPNode,
        sourceNode: XMPNode,
        deleteEmptyValues: Boolean
    ) {

        val sourceItems = sourceNode.iterateChildren()

        while (sourceItems.hasNext()) {

            val sourceItem = sourceItems.next()

            if (sourceItem.getQualifierLength() == 0 ||
                XMPConst.XML_LANG != sourceItem.getQualifier(1).name
            )
                continue

            val lang = sourceItem.getQualifier(1).value ?: ""

            val destIndex = lookupLanguageItem(destNode, lang)

            if (deleteEmptyValues && sourceItem.value.isNullOrEmpty()) {

                if (destIndex != -1) {

                    destNode.removeChild(destIndex)

                    if (!destNode.hasChildren())
                        destParent.removeChild(destNode)
                }
            } else if (destIndex == -1) {

                /* Not replacing: keep the existing x-default item. */
                if (XMPConst.X_DEFAULT != lang || !destNode.hasChildren()) {

                    destNode.addChild(sourceItem.clone())

                } else {

                    val destItem = XMPNode(sourceItem.name, sourceItem.value, sourceItem.options)

                    sourceItem.cloneSubtree(destItem)

                    destNode.addChild(1, destItem)
                }
            }
        }
    }

    /**
     * Merges other arrays by item values. Order and duplicates don't matter, and source
     * items with empty values do not cause deletion, that conflicts horribly with
     * merging.
     *
     * @param destNode   the destination array.
     * @param sourceNode the source array.
     */
    private fun mergeArrayByItems(destNode: XMPNode, sourceNode: XMPNode) {

        val sourceItems = sourceNode.iterateChildren()

        while (sourceItems.hasNext()) {

            val sourceItem = sourceItems.next()

            var match = false

            val destItems = destNode.iterateChildren()

            while (destItems.hasNext()) {

                if (itemValuesMatch(sourceItem, destItems.next())) {
                    match = true
                    break
                }
            }

            if (!match)
                destNode.addChild(sourceItem.clone())
        }
    }

    /**
     * Compares two array items including children and qualifiers, ignoring the order of
     * struct fields and array items.
     *
     * @param leftNode  the source item.
     * @param rightNode the destination item.
     * @return Returns true if the items are equal.
     */
    private fun itemValuesMatch(leftNode: XMPNode, rightNode: XMPNode): Boolean {

        val leftForm = leftNode.options
        val rightForm = rightNode.options

        if (leftForm.getOptions() != rightForm.getOptions())
            return false

        if (leftForm.getOptions() == 0) {

            /* Simple nodes: check the values and xml:lang qualifiers. */

            if (leftNode.value != rightNode.value)
                return false

            if (leftForm.hasLanguage() != rightForm.hasLanguage())
                return false

            if (leftForm.hasLanguage() &&
                leftNode.getQualifier(1).value != rightNode.getQualifier(1).value
            )
                return false

        } else if (leftForm.isStruct()) {

            /* Struct nodes: see if all fields match, ignoring order. */

            if (leftNode.getChildrenLength() != rightNode.getChildrenLength())
                return false

            val leftFields = leftNode.iterateChildren()

            while (leftFields.hasNext()) {

                val leftField = leftFields.next()

                val rightField = findChildNode(rightNode, leftField.name, false)

                if (rightField == null || !itemValuesMatch(leftField, rightField))
                    return false
            }

        } else {

            /*
             * Array nodes: the left values are present in the right node, ignoring order,
             * duplicates and extra values in the right node, which is the destination.
             */

            val leftItems = leftNode.iterateChildren()

            while (leftItems.hasNext()) {

                val leftItem = leftItems.next()

                var match = false

                val rightItems = rightNode.iterateChildren()

                while (rightItems.hasNext()) {

                    if (itemValuesMatch(leftItem, rightItems.next())) {
                        match = true
                        break
                    }
                }

                if (!match)
                    return false
            }
        }

        return true
    }

    /**
     * Splits a concatenated string into the items of an array, like the Adobe original:
     * values are separated by spaces, semicolons and commas, quoted values keep their
     * separators and doubled quotes undouble, and a matching old value is kept instead of
     * being duplicated.
     * @param xmp              the metadata object that holds the array.
     * @param schemaNS         the schema namespace of the array.
     * @param arrayName        the name of the array.
     * @param separatedValues  the concatenated values.
     * @param arrayOptions     the array form for a newly created array, null for a bag.
     * @param preserveCommas   keep commas within values instead of separating on them.
     * @throws XMPException If the array exists in an incompatible form or the options
     * contain non-array bits.
     */
    public fun separateArrayItems(
        xmp: XMPMeta,
        schemaNS: String,
        arrayName: String,
        separatedValues: String,
        arrayOptions: PropertyOptions?,
        preserveCommas: Boolean
    ) {

        val arrayNode = separateFindCreateArray(schemaNS, arrayName, arrayOptions, xmp)

        /* Extract the item values one at a time, until the whole input string is done. */
        val endPos = separatedValues.length
        var itemEnd = 0

        while (itemEnd < endPos) {

            /*
             * Skip any leading spaces and separation characters. Always skip commas here:
             * they can be kept when within a value, but not when alone between values.
             */
            var itemStart = itemEnd
            var ch = ' '
            var charKind = UCK_NORMAL

            for (pos in itemEnd until endPos) {

                ch = separatedValues[pos]
                charKind = classifyCharacter(ch)
                itemStart = pos

                if (charKind == UCK_NORMAL || charKind == UCK_QUOTE)
                    break
            }

            if (itemStart >= endPos)
                break

            val itemValue: String

            if (charKind != UCK_QUOTE) {

                /* This is not a quoted value. Scan for the end, create an array item from
                 * the substring. */
                itemEnd = scanPlainValue(separatedValues, itemStart, preserveCommas)

                itemValue = separatedValues.substring(itemStart, itemEnd)
            } else {

                /*
                 * This is a quoted value: accumulate it, undoubling internal quotes that
                 * match the surrounding quotes, and leave itemEnd behind the closing
                 * quote.
                 */
                val quoted = scanQuotedValue(separatedValues, itemStart + 1, ch)

                itemValue = quoted.first
                itemEnd = quoted.second
            }

            /*
             * Add the separated item to the array. Keep a matching old value in case it
             * had separators.
             */
            var foundIndex = -1

            for (oldChild in 1..arrayNode.getChildrenLength()) {

                if (itemValue == arrayNode.getChild(oldChild).value) {
                    foundIndex = oldChild
                    break
                }
            }

            if (foundIndex < 0)
                arrayNode.addChild(XMPNode(XMPConst.ARRAY_ITEM_NAME, itemValue))
        }
    }

    /**
     * Checks whether a character kind forces quoting of the value.
     *
     * @param charKind    the character kind to check.
     * @param allowCommas count commas as part of the values instead of as separators.
     * @return Returns true when the character kind requires quoting.
     */
    private fun isSeparatorNeedingQuotes(charKind: Int, allowCommas: Boolean): Boolean =
        charKind == UCK_SEMICOLON || charKind == UCK_CONTROL ||
            (charKind == UCK_COMMA && !allowCommas)

    /**
     * Checks whether a character kind belongs to the value during a scan, which depends
     * on whether commas are preserved.
     *
     * @param charKind       the character kind to check.
     * @param preserveCommas keep commas within values instead of separating on them.
     * @return Returns true when the character continues the current value.
     */
    private fun isValueCharacter(charKind: Int, preserveCommas: Boolean): Boolean =
        charKind == UCK_NORMAL || charKind == UCK_QUOTE ||
            (charKind == UCK_COMMA && preserveCommas)

    /**
     * Scans a non-quoted value: value characters continue, a single space continues when
     * a value character follows, and multiple spaces or a separator end the value.
     *
     * @param separatedValues the full input string.
     * @param itemStart       the position where the value starts.
     * @param preserveCommas  keep commas within values instead of separating on them.
     * @return Returns the position behind the value.
     */
    private fun scanPlainValue(
        separatedValues: String,
        itemStart: Int,
        preserveCommas: Boolean
    ): Int {

        val endPos = separatedValues.length

        var itemEnd = itemStart

        while (itemEnd < endPos && continuesValue(separatedValues, itemEnd, preserveCommas))
            itemEnd++

        return itemEnd
    }

    /**
     * Checks whether the character at the position continues the current value: a value
     * character, or a single space followed by a value character.
     *
     * @param separatedValues the full input string.
     * @param position        the position to check.
     * @param preserveCommas  keep commas within values instead of separating on them.
     * @return Returns true when the scan continues through this position.
     */
    private fun continuesValue(
        separatedValues: String,
        position: Int,
        preserveCommas: Boolean
    ): Boolean {

        val charKind = classifyCharacter(separatedValues[position])

        if (isValueCharacter(charKind, preserveCommas))
            return true

        if (charKind != UCK_SPACE)
            return false

        if (position + 1 >= separatedValues.length)
            return false

        return isValueCharacter(classifyCharacter(separatedValues[position + 1]), preserveCommas)
    }

    /**
     * Accumulates a quoted value into a local string, undoubling internal quotes that
     * match the surrounding quotes. Unmatching quotes are copied as-is, and edge cases
     * like undoubled opening quotes or the end of the input are tolerated, like the
     * Adobe original does.
     *
     * @param separatedValues the full input string.
     * @param itemStart       the position behind the opening quote.
     * @param openQuote       the opening quote character.
     * @return Returns the unquoted value and the position behind the closing quote.
     */
    private fun scanQuotedValue(
        separatedValues: String,
        itemStart: Int,
        openQuote: Char
    ): Pair<String, Int> {

        val endPos = separatedValues.length
        val closeQuote = getClosingQuote(openQuote)

        var itemEnd = itemStart

        val builder = StringBuilder()

        while (itemEnd < endPos) {

            val ch = separatedValues[itemEnd]
            val charKind = classifyCharacter(ch)

            if (charKind != UCK_QUOTE || !isSurroundingQuote(ch, openQuote, closeQuote)) {

                /* This is not a matching quote, just append it to the item value. */
                builder.append(ch)
            } else {

                /* This is a "matching" quote. Is it doubled, or the final closing quote? */

                val nextChar: Char
                val nextKind: Int

                if (itemEnd + 1 < endPos) {
                    nextChar = separatedValues[itemEnd + 1]
                    nextKind = classifyCharacter(nextChar)
                } else {
                    nextKind = UCK_SEMICOLON
                    nextChar = ';'
                }

                if (ch == nextChar) {

                    /* This is doubled: copy it and skip the double. */
                    builder.append(ch)
                    itemEnd++
                } else if (!isClosingQuote(ch, openQuote, closeQuote)) {

                    /* This is an undoubled, non-closing quote, copy it. */
                    builder.append(ch)
                } else {

                    /* This is an undoubled closing quote: skip it and exit. */
                    itemEnd++
                    return Pair(builder.toString(), itemEnd)
                }
            }

            itemEnd++
        }

        return Pair(builder.toString(), itemEnd)
    }

    /**
     * Finds or creates the array used by [separateArrayItems], verifying that an existing
     * array is a non-alternate array of a compatible form.
     *
     * @param schemaNS     the schema namespace of the array.
     * @param arrayName    the name of the array.
     * @param arrayOptions the array form for a newly created array.
     * @param xmp          the metadata object.
     * @return Returns the array node.
     * @throws XMPException If the existing array is incompatible or the options are not
     * pure array options.
     */
    private fun separateFindCreateArray(
        schemaNS: String,
        arrayName: String,
        arrayOptions: PropertyOptions?,
        xmp: XMPMeta
    ): XMPNode {

        val verifiedOptions = verifySetOptions(arrayOptions ?: PropertyOptions(), null)

        if (!verifiedOptions.isOnlyArrayOptions())
            throw XMPException("Options can only provide array form", XMPErrorConst.BADOPTIONS)

        val arrayPath = expandXPath(schemaNS, arrayName)

        val arrayNode = findNode(xmp.root, arrayPath, false, null)

        if (arrayNode != null) {

            /* The array exists, make sure the form is compatible. */
            val arrayForm = arrayNode.options

            if (!arrayForm.isArray() || arrayForm.isArrayAlternate())
                throw XMPException("Named property must be non-alternate array", XMPErrorConst.BADXPATH)

            /*
             * The Java 5.1.3 original throws on equal forms (an inversion, flagged with
             * "*** Right error?" in the Adobe source); the C++ original rejects mismatched
             * forms, which is the sensible contract and implemented here.
             */
            if (!verifiedOptions.equalArrayTypes(arrayForm))
                throw XMPException("Mismatch of specified and existing array form", XMPErrorConst.BADXPATH)

            return arrayNode
        }

        /* The array does not exist, try to create it. */
        return findNode(xmp.root, arrayPath, true, verifiedOptions.setArray(true))
            ?: throw XMPException("Failed to create named array", XMPErrorConst.BADXPATH)
    }

    /**
     * Classifies a character into normal characters, spaces, commas, semicolons, quotes
     * and control characters, like the Adobe original.
     *
     * @param ch the character to classify.
     * @return Returns the character kind constant.
     */
    private fun classifyCharacter(ch: Char): Int {

        if (ch in SPACES || ch.code in SPACE_RANGE)
            return UCK_SPACE

        if (ch in COMMAS)
            return UCK_COMMA

        if (ch in SEMICOLA)
            return UCK_SEMICOLON

        if (ch in QUOTES || ch.code in BRACKET_QUOTE_RANGE || ch.code in TYPOGRAPHIC_QUOTE_RANGE)
            return UCK_QUOTE

        if (ch.code < FIRST_ASCII_CONTROL || ch in CONTROLS)
            return UCK_CONTROL

        /* Assume typical case. */
        return UCK_NORMAL
    }

    /**
     * Returns the matching closing quote for an opening quote, or the zero character for
     * an undirected quote.
     *
     * @param openQuote the opening quote character.
     * @return Returns the closing quote character.
     */
    private fun getClosingQuote(openQuote: Char): Char =
        when (openQuote) {
            '\u0022' -> '\u0022'
            '\u00AB' -> '\u00BB'
            '\u00BB' -> '\u00AB'
            '\u2015' -> '\u2015'
            '\u2018' -> '\u2019'
            '\u201A' -> '\u201B'
            '\u201C' -> '\u201D'
            '\u201E' -> '\u201F'
            '\u2039' -> '\u203A'
            '\u203A' -> '\u2039'
            '\u3008' -> '\u3009'
            '\u300A' -> '\u300B'
            '\u300C' -> '\u300D'
            '\u300E' -> '\u300F'
            '\u301D' -> '\u301F'
            else -> Char.MIN_VALUE
        }

    /**
     * Checks whether a character opens or closes the given quote pair.
     *
     * @param ch         the character to check.
     * @param openQuote  the opening quote character.
     * @param closeQuote the closing quote character.
     * @return Returns true for both quote characters of the pair.
     */
    private fun isSurroundingQuote(ch: Char, openQuote: Char, closeQuote: Char): Boolean =
        ch == openQuote || isClosingQuote(ch, openQuote, closeQuote)

    /**
     * Checks whether a character closes the given quote pair. U+301E closes U+301D as
     * well as U+301F.
     *
     * @param ch         the character to check.
     * @param openQuote  the opening quote character.
     * @param closeQuote the closing quote character.
     * @return Returns true for the closing quote characters.
     */
    private fun isClosingQuote(ch: Char, openQuote: Char, closeQuote: Char): Boolean =
        ch == closeQuote || (openQuote == '\u301D' && (ch == '\u301E' || ch == '\u301F'))

    /**
     * Catenates the items of an array into one string, quoting items that contain
     * separators so that [separateArrayItems] round trips them back.
     *
     * @param xmp         the metadata object that holds the array.
     * @param schemaNS    the schema namespace of the array.
     * @param arrayName   the name of the array.
     * @param separator   the separator string with exactly one semicolon, "; " by default.
     * @param quotes      the opening and closing quote pair, "\"" by default.
     * @param allowCommas count commas as part of the values instead of as separators.
     * @return Returns the concatenated string, empty when the array does not exist.
     * @throws XMPException If the property is not a non-alternate array, an item is not
     * simple, or separator and quotes are invalid.
     */
    public fun catenateArrayItems(
        xmp: XMPMeta,
        schemaNS: String,
        arrayName: String,
        separator: String?,
        quotes: String?,
        allowCommas: Boolean
    ): String {

        val actualSeparator = if (separator.isNullOrEmpty()) "; " else separator
        val actualQuotes = if (quotes.isNullOrEmpty()) "\"" else quotes

        /* Return an empty result if the array does not exist, hurl if it isn't the right
         * form. */
        val arrayPath = expandXPath(schemaNS, arrayName)

        val arrayNode = findNode(xmp.root, arrayPath, false, null)
            ?: return ""

        if (!arrayNode.options.isArray() || arrayNode.options.isArrayAlternate())
            throw XMPException("Named property must be non-alternate array", XMPErrorConst.BADPARAM)

        /* Make sure the separator is OK. */
        checkSeparator(actualSeparator)

        /* Make sure the open and close quotes are a legitimate pair. */
        val openQuote = actualQuotes[0]
        val closeQuote = checkQuotes(actualQuotes, openQuote)

        /* Build the result, quoting the array items and adding separators. Hurl if any
         * item isn't simple. */
        val catenated = StringBuilder()

        val items = arrayNode.iterateChildren()

        while (items.hasNext()) {

            val currItem = items.next()

            if (currItem.options.isCompositeProperty())
                throw XMPException("Array items must be simple", XMPErrorConst.BADPARAM)

            catenated.append(applyQuotes(currItem.value, openQuote, closeQuote, allowCommas))

            if (items.hasNext())
                catenated.append(actualSeparator)
        }

        return catenated.toString()
    }

    /**
     * Checks that the separator is one semicolon surrounded by zero or more spaces, any
     * of the recognized semicolons and spaces.
     *
     * @param separator the separator string.
     * @throws XMPException If the separator has not exactly one semicolon.
     */
    private fun checkSeparator(separator: String) {

        var haveSemicolon = false

        for (ch in separator) {

            val charKind = classifyCharacter(ch)

            if (charKind == UCK_SEMICOLON) {

                if (haveSemicolon)
                    throw XMPException("Separator can have only one semicolon", XMPErrorConst.BADPARAM)

                haveSemicolon = true
            } else if (charKind != UCK_SPACE) {
                throw XMPException(
                    "Separator can have only spaces and one semicolon",
                    XMPErrorConst.BADPARAM
                )
            }
        }

        if (!haveSemicolon)
            throw XMPException("Separator must have one semicolon", XMPErrorConst.BADPARAM)
    }

    /**
     * Checks that the open and close quotes are a legitimate pair and returns the correct
     * closing quote.
     *
     * @param quotes    the opening and closing quote in a string.
     * @param openQuote the opening quote.
     * @return Returns the corresponding closing quote.
     * @throws XMPException If the quoting characters are invalid or the pair mismatches.
     */
    private fun checkQuotes(quotes: String, openQuote: Char): Char {

        val charKind = classifyCharacter(openQuote)

        if (charKind != UCK_QUOTE)
            throw XMPException("Invalid quoting character", XMPErrorConst.BADPARAM)

        val closeQuote = if (quotes.length == 1) {
            openQuote
        } else {
            val explicitCloseQuote = quotes[1]

            if (classifyCharacter(explicitCloseQuote) != UCK_QUOTE)
                throw XMPException("Invalid quoting character", XMPErrorConst.BADPARAM)

            explicitCloseQuote
        }

        if (closeQuote != getClosingQuote(openQuote))
            throw XMPException("Mismatched quote pair", XMPErrorConst.BADPARAM)

        return closeQuote
    }

    /**
     * Quotes the item when it contains separators, doubling internal quotes that match
     * the outer pair. Internal quotes alone, as in - Irving "Bud" Jones - do not need
     * quoting; a leading quote would make the value look quoted and is quoted therefore.
     *
     * Note: The Java 5.1.3 original scans item.charAt(i) instead of
     * item.charAt(splitPoint) in the quote-search; this port implements the intended scan
     * of the Adobe C++ original.
     *
     * @param item        the value to quote, null is treated as empty.
     * @param openQuote   the opening quote character.
     * @param closeQuote  the closing quote character.
     * @param allowCommas flag if commas are allowed unquoted.
     * @return Returns the value in quotes when quoting is needed.
     */
    private fun applyQuotes(
        item: String?,
        openQuote: Char,
        closeQuote: Char,
        allowCommas: Boolean
    ): String {

        val value = item ?: ""

        var prevSpace = false

        /* See if there are any separators in the value, stopping at the first occurrence.
         * The purpose of quoting is that catenate and separate round trip properly. */

        var i = 0

        while (i < value.length) {

            val charKind = classifyCharacter(value[i])

            if (i == 0 && charKind == UCK_QUOTE)
                break

            if (charKind == UCK_SPACE) {

                /* Multiple spaces are a separator. */
                if (prevSpace)
                    break

                prevSpace = true
            } else {

                prevSpace = false

                if (isSeparatorNeedingQuotes(charKind, allowCommas))
                    break
            }

            i++
        }

        if (i < value.length) {

            /* Create a quoted copy, doubling any internal quotes that match the outer
             * ones. Rescan the front of the string for quotes. */

            var splitPoint = 0

            while (splitPoint < i && classifyCharacter(value[splitPoint]) != UCK_QUOTE)
                splitPoint++

            val newItem = StringBuilder(value.length + 2)

            newItem.append(openQuote).append(value, 0, splitPoint)

            for (charOffset in splitPoint until value.length) {

                newItem.append(value[charOffset])

                if (classifyCharacter(value[charOffset]) == UCK_QUOTE &&
                    isSurroundingQuote(value[charOffset], openQuote, closeQuote)
                )
                    newItem.append(value[charOffset])
            }

            newItem.append(closeQuote)

            return newItem.toString()
        }

        return value
    }
}
