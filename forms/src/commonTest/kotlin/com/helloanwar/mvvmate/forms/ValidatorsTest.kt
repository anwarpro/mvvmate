package com.helloanwar.mvvmate.forms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidatorsTest {

    @Test
    fun testRequired() {
        val validator = Validators.required<String>("req")
        assertEquals("req", validator(null))
        assertEquals("req", validator(""))
        assertEquals("req", validator("   "))
        assertNull(validator("valid"))
    }

    @Test
    fun testMinLength() {
        val validator = Validators.minLength(3, "min 3")
        assertNull(validator(null)) // Nulls pass unless required
        assertEquals("min 3", validator("ab"))
        assertNull(validator("abc"))
        assertNull(validator("abcd"))
    }

    @Test
    fun testMaxLength() {
        val validator = Validators.maxLength(3, "max 3")
        assertNull(validator(null))
        assertNull(validator("a"))
        assertNull(validator("abc"))
        assertEquals("max 3", validator("abcd"))
    }

    @Test
    fun testEmail() {
        val validator = Validators.email("bad email")
        assertNull(validator(null))
        assertNull(validator(""))
        assertEquals("bad email", validator("invalid"))
        assertEquals("bad email", validator("test@"))
        assertEquals("bad email", validator("@test.com"))
        assertNull(validator("test@demo.com"))
        assertNull(validator("a.b@c.co.uk"))
    }

    @Test
    fun testPattern() {
        val validator = Validators.pattern("^[A-Z]+$".toRegex(), "only uppercase")
        assertNull(validator(null))
        assertNull(validator(""))
        assertEquals("only uppercase", validator("abc"))
        assertEquals("only uppercase", validator("123"))
        assertNull(validator("ABC"))
    }

    @Test
    fun testDigitsRequired() {
        val validator = Validators.digitsRequired("digits only")
        assertNull(validator(null))
        assertNull(validator(""))
        assertEquals("digits only", validator("123a"))
        assertEquals("digits only", validator("12.3"))
        assertNull(validator("12345"))
    }

    @Test
    fun testDecimalRequired() {
        val validator = Validators.decimalRequired("decimal only")
        assertNull(validator(null))
        assertNull(validator(""))
        assertEquals("decimal only", validator("abc"))
        assertEquals("decimal only", validator("12..3"))
        assertNull(validator("123"))
        assertNull(validator("12.34"))
        assertNull(validator("-12.34"))
    }
}
