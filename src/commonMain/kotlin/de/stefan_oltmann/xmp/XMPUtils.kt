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
import de.stefan_oltmann.xmp.internal.XMPNodeUtils.findNode
import de.stefan_oltmann.xmp.internal.XMPNodeUtils.findSchemaNode
import de.stefan_oltmann.xmp.internal.XMPPath
import de.stefan_oltmann.xmp.internal.XMPPathParser.expandXPath

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
}
