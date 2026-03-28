package de.stefan_oltmann.xmp

/** As used in Iptc4xmpExt */
public data class XMPLocation(

    /**
     * Iptc4xmpExt:LocationName
     *
     * For example "Times Square"
     */
    val name: String?,

    /**
     * Iptc4xmpExt:Sublocation
     *
     * For example "Schnurpselstraße 7"
     */
    val location: String?,

    /**
     * Iptc4xmpExt:City
     * photoshop:City
     *
     * For example "Oldenburg"
     */
    val city: String?,

    /**
     * Iptc4xmpExt:ProvinceState
     * photoshop:State
     *
     * For example "Niedersachsen"
     */
    val state: String?,

    /**
     * Iptc4xmpExt:CountryName
     * photoshop:Country
     *
     * For example "Deutschland"
     */
    val country: String?
)
