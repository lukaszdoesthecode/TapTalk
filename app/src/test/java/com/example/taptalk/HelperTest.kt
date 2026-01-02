package com.example.taptalk

import androidx.compose.ui.graphics.Color
import com.example.taptalk.ui.theme.Noun
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HelpersTest {

    @Test
    fun `negativeIconFor returns correct icons`() {
        assertTrue(negativeIconFor("won't go").contains("negative_future"))
        assertTrue(negativeIconFor("hasn't done").contains("negative_perfect"))
        assertTrue(negativeIconFor("didn't go").contains("negative_past"))
        assertTrue(negativeIconFor("don't know").contains("negative_present"))
        assertTrue(negativeIconFor("random").contains("negative_present"))
    }

    @Test
    fun `specialVerbForms handles be have will`() {
        val be = specialVerbForms("be")
        assertEquals("be", be?.base)
        assertTrue(be?.negatives?.contains("am not") == true)

        val have = specialVerbForms("have")
        assertEquals("have", have?.base)
        assertEquals("had", have?.past)

        val will = specialVerbForms("will")
        assertEquals("will", will?.base)
        assertTrue(will?.negatives?.contains("won’t") == true)

        assertNull(specialVerbForms("walk"))
    }

    @Test
    fun `getVerbForms loads from json`() {
        val json = JSONObject(
            """
                {
                    "walk": {
                        "past": "walked",
                        "perfect": "walked",
                        "negatives": ["don't walk"]
                    }
                }
            """
        )

        val forms = getVerbForms("walk", json)

        assertEquals("walk", forms.base)
        assertEquals("walked", forms.past)
        assertEquals("walked", forms.perfect)
        assertEquals(1, forms.negatives.size)
    }

    @Test
    fun `getVerbForms fallback when not in json`() {
        val json = JSONObject("{}")

        val forms = getVerbForms("jump", json)

        assertEquals("jump", forms.base)
        assertEquals("jumped", forms.past)
        assertEquals("jumped", forms.perfect)
    }

    @Test
    fun `normalizeFileName cleans filename correctly`() {
        val result = normalizeFileName("123_good_morning_A1.png")
        assertEquals("Good Morning", result)

        val result2 = normalizeFileName("the_house.png")
        assertEquals("The House", result2)
    }

    @Test
    fun `suggestPlural irregular from json`() {
        val json = JSONObject("""{"mouse": "mice"}""")

        assertEquals("Mice", suggestPlural("mouse", json))
    }

    @Test
    fun `suggestPlural handles y ending`() {
        val json = JSONObject("{}")

        assertEquals("Babies", suggestPlural("baby", json))
    }

    @Test
    fun `suggestPlural s x z ch sh endings`() {
        val json = JSONObject("{}")

        assertEquals("Boxes", suggestPlural("box", json))
        assertEquals("Buzzes", suggestPlural("buzz", json))
        assertEquals("Watches", suggestPlural("watch", json))
        assertEquals("Bushes", suggestPlural("bush", json))
    }

    @Test
    fun `suggestPlural default plural`() {
        val json = JSONObject("{}")

        assertEquals("Cats", suggestPlural("Cat", json))
    }

    @Test
    fun `borderColorFor returns color for known folders`() {
        // these objects exist in your theme package
        assertNotEquals(Color.Black, borderColorFor("nouns"))
        assertNotEquals(Color.Black, borderColorFor("verbs"))

        // unknown → black
        assertEquals(Color.Black, borderColorFor("unknown_thing"))
    }

    @Test
    fun `negativeIconFor returns future negative icon`() {
        val result = negativeIconFor("I WON'T GO")
        assertTrue(result.endsWith("negative_future.png"))
    }

    @Test
    fun `negativeIconFor returns perfect negative icon`() {
        val result = negativeIconFor("he hasn't eaten")
        assertTrue(result.endsWith("negative_perfect.png"))
    }

    @Test
    fun `negativeIconFor returns past negative icon`() {
        val result = negativeIconFor("they didn't go")
        assertTrue(result.endsWith("negative_past.png"))
    }

    @Test
    fun `negativeIconFor returns present negative icon by default`() {
        val result = negativeIconFor("doesn't work")
        assertTrue(result.endsWith("negative_present.png"))
    }

    @Test
    fun `specialVerbForms handles be`() {
        val result = specialVerbForms("Be")
        assertEquals("be", result!!.base)
        assertEquals("was/were", result.past)
    }

    @Test
    fun `specialVerbForms handles will`() {
        val result = specialVerbForms("WILL")
        assertEquals("will", result!!.base)
        assertEquals("would", result.past)
    }

    @Test
    fun `getVerbForms returns fallback when verb not in JSON`() {
        val json = JSONObject("{}")
        val result = getVerbForms("jump", json)
        assertEquals("jumped", result.past)
    }

    @Test
    fun `borderColorFor returns noun color`() {
        val result = borderColorFor("Nouns")
        assertEquals(Noun, result)
    }

    @Test
    fun `borderColorFor returns black for unknown folder`() {
        val result = borderColorFor("something_random")
        assertEquals(Color.Black, result)
    }


    @Test
    fun `normalizeFileName removes CEFR suffix`() {
        val name = normalizeFileName("eat_A1.png")
        assertEquals("Eat", name)
    }

    @Test
    fun `normalizeFileName removes leading numbers`() {
        val name = normalizeFileName("12_hello.png")
        assertEquals("Hello", name)
    }

    @Test
    fun `suggestPlural uses irregular json value`() {
        val json = JSONObject("""{"mouse": "mice"}""")
        val result = suggestPlural("mouse", json)
        assertEquals("Mice", result)
    }

    @Test
    fun `suggestPlural converts consonant Y ending`() {
        val json = JSONObject("{}")
        val result = suggestPlural("city", json)
        assertEquals("Cities", result)
    }

    @Test
    fun `suggestPlural adds es for s x ch sh`() {
        val json = JSONObject("{}")
        val result = suggestPlural("box", json)
        assertEquals("Boxes", result)
    }


}
