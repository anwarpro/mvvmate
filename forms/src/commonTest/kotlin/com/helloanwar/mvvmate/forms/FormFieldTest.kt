package com.helloanwar.mvvmate.forms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormFieldTest {

    @Test
    fun testInitialState() {
        val field = FormField("test")
        assertEquals("test", field.value)
        assertTrue(field.errors.isEmpty())
        assertFalse(field.isTouched)
        assertFalse(field.isDirty)
        assertTrue(field.isValid)
        assertFalse(field.isInvalid)
    }

    @Test
    fun testSetValueWithSuccessValidation() {
        val field = FormField("")
        
        val updatedField = field.setValue("new value", Validators.required())
        
        assertEquals("new value", updatedField.value)
        assertTrue(updatedField.errors.isEmpty())
        assertTrue(updatedField.isTouched)
        assertTrue(updatedField.isDirty)
        assertTrue(updatedField.isValid)
    }

    @Test
    fun testSetValueWithFailingValidation() {
        val field = FormField("initial")
        
        val updatedField = field.setValue("", Validators.required("cannot be empty"))
        
        assertEquals("", updatedField.value)
        assertEquals(1, updatedField.errors.size)
        assertEquals("cannot be empty", updatedField.errors.first())
        assertTrue(updatedField.isTouched)
        assertTrue(updatedField.isDirty)
        assertFalse(updatedField.isValid)
        assertTrue(updatedField.isInvalid)
    }

    @Test
    fun testMarkTouchedAppliesValidation() {
        val field = FormField("")
        
        val touchedField = field.markTouched(Validators.required("required field"))
        
        assertEquals("", touchedField.value)
        assertEquals(1, touchedField.errors.size)
        assertEquals("required field", touchedField.errors.first())
        assertTrue(touchedField.isTouched)
        assertFalse(touchedField.isDirty) // Value hasn't changed
    }
}
