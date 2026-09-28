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

import de.stefan_oltmann.xmp.internal.Utils.isInternalProperty
import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.internal.XMPNode
import de.stefan_oltmann.xmp.internal.XMPNodeUtils.findChildNode
import de.stefan_oltmann.xmp.internal.XMPNodeUtils.findNode
import de.stefan_oltmann.xmp.internal.XMPNodeUtils.findSchemaNode
import de.stefan_oltmann.xmp.internal.XMPNodeUtils.lookupLanguageItem
import de.stefan_oltmann.xmp.internal.XMPPath
import de.stefan_oltmann.xmp.internal.XMPPathParser.expandXPath
import de.stefan_oltmann.xmp.options.PropertyOptions

/**
 * Utility methods to process whole metadata trees, ported from the Adobe XMP Core.
 */
public object XMPUtils {

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
}
