package dev.stevanmosquetti.openplaud.slugify

/**
 * Derives the XMPP localpart (user part of a JID, or MUC room name) from a display name.
 *
 * XMPP localpart rules: 2-30 characters from [a-z0-9._-/]. We keep only alphanumeric
 * characters so the result is always a valid localpart and stable across calls.
 */
object Slugifier {

    /**
     * Returns the alphanumeric slug of the display name; requires a 2-30 character
     * result.
     */
    fun slugify(name: String): String {
        val slug = name.lowercase().filter { it.isLetterOrDigit() }
        require(slug.length in 2..30) {
            "Display name '$name' does not produce a valid XMPP localpart (need 2-30 alphanumeric characters)"
        }
        return slug
    }
}
