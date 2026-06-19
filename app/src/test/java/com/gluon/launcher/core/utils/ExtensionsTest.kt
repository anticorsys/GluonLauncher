package com.gluon.launcher.core.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsTest {

    @Test
    fun `isValidEmail - корректные адреса`() {
        assertTrue("test@example.com".isValidEmail())
        assertTrue("user.name+tag@domain.co".isValidEmail())
        assertTrue("x@y.z".isValidEmail())
        assertTrue("a@b.cd".isValidEmail())
    }

    @Test
    fun `isValidEmail - некорректные адреса`() {
        assertFalse("plainstring".isValidEmail())
        assertFalse("missing@tld".isValidEmail())    // нет точки в домене?
        assertFalse("missing@.com".isValidEmail())   // домен начинается с точки
        assertFalse("@no-local.com".isValidEmail())
        assertFalse("spaces in@email.com".isValidEmail())
        assertFalse("".isValidEmail())
        assertFalse("a@b".isValidEmail())            // домен слишком короткий
    }

    @Test
    fun `isValidEmail - запятая вместо точки`() {
        assertFalse("test@example,com".isValidEmail())
    }
}