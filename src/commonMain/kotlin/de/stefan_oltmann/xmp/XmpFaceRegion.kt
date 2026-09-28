package de.stefan_oltmann.xmp

/**
 * A face region of the mwg-rs:RegionList: the person's name plus the face area.
 *
 * Unlike a name-keyed map, a list of these regions can carry several regions with the
 * same name, because the XMP region model stores a plain array and two people in one
 * photo may share a name. The name is null when the region carries none, so nameless
 * regions survive a read-modify-write cycle instead of being dropped.
 */
public data class XmpFaceRegion(

    /**
     * The name of the person shown, or null when the region carries no name.
     */
    public val name: String?,

    /**
     * The face area, normalized to the dimensions the region list declares.
     */
    public val area: XMPRegionArea
)
