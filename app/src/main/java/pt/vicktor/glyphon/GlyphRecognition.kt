package pt.vicktor.glyphon

class GlyphSignature(nodeIds: List<Int>) {
    val nodeIds: List<Int> = normalizeNodeIds(nodeIds)

    val isValid: Boolean
        get() = nodeIds.size >= MIN_NODE_COUNT

    override fun equals(other: Any?): Boolean {
        return other is GlyphSignature && nodeIds == other.nodeIds
    }

    override fun hashCode(): Int = nodeIds.hashCode()

    override fun toString(): String = "GlyphSignature(nodeIds=$nodeIds)"

    private companion object {
        const val MIN_NODE_COUNT = 2

        fun normalizeNodeIds(nodeIds: List<Int>): List<Int> {
            if (nodeIds.isEmpty()) return emptyList()

            return buildList {
                nodeIds.forEach { nodeId ->
                    if (lastOrNull() != nodeId) {
                        add(nodeId)
                    }
                }
            }
        }
    }
}

class GlyphSequence(glyphs: List<GlyphSignature>) {
    val glyphs: List<GlyphSignature> = glyphs.toList()

    val isValid: Boolean
        get() = glyphs.size in SUPPORTED_GLYPH_COUNTS && glyphs.all(GlyphSignature::isValid)

    override fun equals(other: Any?): Boolean {
        return other is GlyphSequence && glyphs == other.glyphs
    }

    override fun hashCode(): Int = glyphs.hashCode()

    override fun toString(): String = "GlyphSequence(glyphs=$glyphs)"

    private companion object {
        val SUPPORTED_GLYPH_COUNTS = setOf(3, 4, 5)
    }
}

fun glyphSignaturesMatch(
    captured: GlyphSignature,
    expected: GlyphSignature
): Boolean = captured.isValid && expected.isValid && captured == expected

fun glyphSequencesMatch(
    captured: GlyphSequence,
    expected: GlyphSequence
): Boolean = captured.isValid && expected.isValid && captured == expected
