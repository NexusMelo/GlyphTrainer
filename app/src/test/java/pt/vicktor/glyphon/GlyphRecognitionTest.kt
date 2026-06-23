package pt.vicktor.glyphon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphRecognitionTest {

    @Test
    fun emptyGlyphIsInvalid() {
        assertFalse(GlyphSignature(emptyList()).isValid)
    }

    @Test
    fun singleNodeGlyphIsInvalid() {
        assertFalse(GlyphSignature(listOf(1)).isValid)
    }

    @Test
    fun glyphWithTwoOrMoreNodesIsValid() {
        assertTrue(GlyphSignature(listOf(1, 2)).isValid)
        assertTrue(GlyphSignature(listOf(1, 2, 3)).isValid)
    }

    @Test
    fun consecutiveRepeatedNodesAreRemoved() {
        val signature = GlyphSignature(listOf(1, 1, 2, 2, 1, 1))

        assertEquals(listOf(1, 2, 1), signature.nodeIds)
    }

    @Test
    fun differentDirectionDoesNotMatch() {
        val captured = GlyphSignature(listOf(1, 2, 3))
        val expected = GlyphSignature(listOf(3, 2, 1))

        assertFalse(glyphSignaturesMatch(captured, expected))
    }

    @Test
    fun invalidGlyphsDoNotMatch() {
        assertFalse(glyphSignaturesMatch(GlyphSignature(emptyList()), GlyphSignature(emptyList())))
    }

    @Test
    fun threeGlyphSequenceIsValid() {
        assertTrue(GlyphSequence(validGlyphs(3)).isValid)
    }

    @Test
    fun fourGlyphSequenceIsValid() {
        assertTrue(GlyphSequence(validGlyphs(4)).isValid)
    }

    @Test
    fun fiveGlyphSequenceIsValid() {
        assertTrue(GlyphSequence(validGlyphs(5)).isValid)
    }

    @Test
    fun unsupportedSequenceSizeIsInvalid() {
        assertFalse(GlyphSequence(validGlyphs(2)).isValid)
        assertFalse(GlyphSequence(validGlyphs(6)).isValid)
    }

    @Test
    fun equalSequencesMatch() {
        val captured = GlyphSequence(validGlyphs(4))
        val expected = GlyphSequence(validGlyphs(4))

        assertTrue(glyphSequencesMatch(captured, expected))
    }

    @Test
    fun differentSequencesDoNotMatch() {
        val captured = GlyphSequence(validGlyphs(4))
        val expected = GlyphSequence(
            listOf(
                GlyphSignature(listOf(9, 8)),
                GlyphSignature(listOf(2, 3)),
                GlyphSignature(listOf(3, 4)),
                GlyphSignature(listOf(4, 5))
            )
        )

        assertFalse(glyphSequencesMatch(captured, expected))
    }

    private fun validGlyphs(count: Int): List<GlyphSignature> {
        return List(count) { index ->
            GlyphSignature(listOf(index, index + 1))
        }
    }
}
