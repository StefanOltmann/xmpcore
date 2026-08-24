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
import de.stefan_oltmann.xmp.options.PropertyOptions

/**
 * A node in the internally XMP tree, which can be a schema node, a property node, an array node,
 * an array item, a struct node or a qualifier node (without '?').
 */
internal class XMPNode(

    /**
     * name of the node, contains different information depending of the node kind.
     */
    var name: String?,

    /**
     * value of the node, contains different information depending of the node kind.
     */
    var value: String?,

    /**
     * options describing the kind of the node.
     */
    var options: PropertyOptions = PropertyOptions()

) : Comparable<XMPNode> {

    internal var parent: XMPNode? = null
    private var children: MutableList<XMPNode>? = null
    private var qualifier: MutableList<XMPNode>? = null

    /* Internal processing options */

    var isImplicit: Boolean = false
    var hasAliases: Boolean = false
    var isAlias: Boolean = false
    var hasValueChild: Boolean = false

    fun clear() {
        name = null
        value = null
        options = PropertyOptions()
        children = null
        qualifier = null
    }

    /**
     * Returns the children or empty list, if there are none.
     * Will not lazily create the list!
     */
    fun getChildren(): List<XMPNode> =
        children ?: emptyList()

    fun getChild(index: Int): XMPNode =
        getOrCreateChildren()[index - 1]

    fun addChild(node: XMPNode) {

        /*
         * Slightly corrupted files may contain a property more than once.
         * Instead of rejecting the whole file, we keep only one value. Like
         * ExifTool when extracting duplicated tags, the last occurrence wins
         * and replaces the earlier one at its position.
         */
        val duplicateIndex = findChildIndex(node.name)

        if (duplicateIndex >= 0) {
            node.parent = this
            getOrCreateChildren()[duplicateIndex] = node
            return
        }

        node.parent = this

        getOrCreateChildren().add(node)
    }

    fun addChild(index: Int, node: XMPNode) {

        /* See addChild(XMPNode): a duplicated name keeps the last occurrence. */
        val duplicateIndex = findChildIndex(node.name)

        if (duplicateIndex >= 0) {
            node.parent = this
            getOrCreateChildren()[duplicateIndex] = node
            return
        }

        node.parent = this

        getOrCreateChildren().add(index - 1, node)
    }

    /**
     * Replaces a node with another one.
     */
    fun replaceChild(index: Int, node: XMPNode) {

        node.parent = this

        getOrCreateChildren()[index - 1] = node
    }

    fun removeChild(itemIndex: Int) {

        getOrCreateChildren().removeAt(itemIndex - 1)

        cleanupChildren()
    }

    /**
     * Removes a child node.
     * If its a schema node and doesn't have any children anymore, its deleted.
     */
    fun removeChild(node: XMPNode) {

        getOrCreateChildren().remove(node)

        cleanupChildren()
    }

    /**
     * Removes the children list if this node has no children anymore;
     * checks if the provided node is a schema node and doesn't have any children anymore, its deleted.
     */
    private fun cleanupChildren() {

        if (children?.isEmpty() == true)
            children = null
    }

    /**
     * Removes all children from the node.
     */
    fun removeChildren() {
        children = null
    }

    fun getChildrenLength(): Int =
        children?.size ?: 0

    fun findChildByName(expr: String?): XMPNode? =
        getOrCreateChildren().find { it.name == expr }

    /**
     * Returns the qualifier or empty list, if there are none.
     * Will not lazily create the list!
     */
    fun getQualifier(): List<XMPNode> =
        qualifier ?: emptyList()

    fun getQualifier(index: Int): XMPNode =
        getOrCreateQualifier()[index - 1]

    fun getQualifierLength(): Int =
        qualifier?.size ?: 0

    fun addQualifier(qualNode: XMPNode) {

        /*
         * See addChild(XMPNode): a duplicated qualifier keeps the last
         * occurrence instead of rejecting the file. The replacement stays at
         * the position of the old qualifier, so "xml:lang" and "rdf:type"
         * keep their fixed places.
         */
        val duplicateIndex = findQualifierIndex(qualNode.name)

        if (duplicateIndex >= 0) {
            qualNode.parent = this
            qualNode.options.setQualifier(true)
            options.setHasQualifiers(true)
            if (XMPConst.XML_LANG == qualNode.name)
                options.setHasLanguage(true)
            else if (XMPConst.RDF_TYPE == qualNode.name)
                options.setHasType(true)
            getOrCreateQualifier()[duplicateIndex] = qualNode
            return
        }

        qualNode.parent = this
        qualNode.options.setQualifier(true)

        options.setHasQualifiers(true)

        /* Contraints */
        if (XMPConst.XML_LANG == qualNode.name) {

            /* "xml:lang" is always first and the option "hasLanguage" is set */
            options.setHasLanguage(true)

            getOrCreateQualifier().add(0, qualNode)

        } else if (XMPConst.RDF_TYPE == qualNode.name) {

            /* "rdf:type" must be first or second after "xml:lang" and the option "hasType" is set */
            options.setHasType(true)

            getOrCreateQualifier().add(
                if (!options.hasLanguage()) 0 else 1,
                qualNode
            )

        } else {

            /* Other qualifiers are appended */
            getOrCreateQualifier().add(qualNode)
        }
    }

    /**
     * Removes one qualifier node and fixes the options.
     */
    fun removeQualifier(qualNode: XMPNode) {

        if (XMPConst.XML_LANG == qualNode.name) {
            /* If "xml:lang" is removed, remove hasLanguage-flag too */
            options.setHasLanguage(false)
        } else if (XMPConst.RDF_TYPE == qualNode.name) {
            /* If "rdf:type" is removed, remove hasType-flag too */
            options.setHasType(false)
        }

        val qualifierList = getOrCreateQualifier()

        qualifierList.remove(qualNode)

        if (qualifierList.isEmpty()) {
            options.setHasQualifiers(false)
            qualifier = null
        }
    }

    /**
     * Removes all qualifiers from the node and sets the options appropriate.
     */
    fun removeQualifiers() {

        /* Clear qualifier related options */
        options.setHasQualifiers(false)
        options.setHasLanguage(false)
        options.setHasType(false)

        qualifier = null
    }

    fun findQualifierByName(expr: String?): XMPNode? =
        qualifier?.find { it.name == expr }

    fun hasChildren(): Boolean =
        children?.isNotEmpty() ?: false

    fun iterateChildren(): Iterator<XMPNode> =
        children?.iterator() ?: emptySequence<XMPNode>().iterator()

    fun iterateChildrenMutable(): MutableIterator<XMPNode> =
        children?.listIterator() ?: mutableListOf<XMPNode>().listIterator()

    fun hasQualifier(): Boolean =
        qualifier?.isNotEmpty() ?: false

    fun iterateQualifier(): Iterator<XMPNode> =
        qualifier?.listIterator() ?: emptySequence<XMPNode>().iterator()

    /**
     * Schema nodes compare by their value (the prefix), all other nodes by name.
     * A null name or value sorts before any non-null one.
     */
    override fun compareTo(other: XMPNode): Int =
        if (options.isSchemaNode())
            compareValuesBy(this, other) { it.value }
        else
            compareValuesBy(this, other) { it.name }

    /**
     * Sorts the complete datamodel according to the following rules:
     *
     *  * Nodes at one level are sorted by name, that is prefix + local name
     *  * Starting at the root node the children and qualifier are sorted recursively,
     * which the following exceptions.
     *  * Sorting will not be used for arrays.
     *  * Within qualifier "xml:lang" and/or "rdf:type" stay at the top in that order, all others are sorted.
     */
    fun sort() {

        /* Sort qualifier */
        if (hasQualifier()) {

            val qualifierList = getOrCreateQualifier()

            val quals = qualifierList.toTypedArray()

            var sortFrom = 0

            while (quals.size > sortFrom &&
                (XMPConst.XML_LANG == quals[sortFrom].name || XMPConst.RDF_TYPE == quals[sortFrom].name)
            ) {
                quals[sortFrom].sort()
                sortFrom++
            }

            quals.sort(sortFrom, quals.size)

            for (index in quals.indices) {
                qualifierList[index] = quals[index]
                quals[index].sort()
            }
        }

        /* Sort children */
        if (hasChildren()) {

            val childList = requireNotNull(children)

            if (!options.isArray())
                childList.sort()

            for (child in childList)
                child.sort()
        }
    }

    /* ------------------------------------------------------------------------------ private methods */

    private fun getOrCreateChildren(): MutableList<XMPNode> =
        children ?: mutableListOf<XMPNode>().also { children = it }

    private fun getOrCreateQualifier(): MutableList<XMPNode> =
        qualifier ?: mutableListOf<XMPNode>().also { qualifier = it }

    /**
     * Returns the index of the child with the given name, or -1 if there is none.
     * Array items share the name "[]" and are never treated as duplicates.
     */
    private fun findChildIndex(childName: String?): Int {

        if (childName == null || XMPConst.ARRAY_ITEM_NAME == childName)
            return -1

        val children = children ?: return -1

        return children.indexOfFirst { it.name == childName }
    }

    /**
     * Returns the index of the qualifier with the given name, or -1 if there is none.
     * Array items share the name "[]" and are never treated as duplicates.
     */
    private fun findQualifierIndex(qualifierName: String?): Int {

        if (qualifierName == null || XMPConst.ARRAY_ITEM_NAME == qualifierName)
            return -1

        val qualifierList = qualifier ?: return -1

        return qualifierList.indexOfFirst { it.name == qualifierName }
    }
}
