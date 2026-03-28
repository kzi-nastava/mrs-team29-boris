package com.example.mobilnaaplikacijatim29.ui.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ResetPasswordFragmentTest {
    @Test
    public void acceptsMatchingStrongPasswordsWithToken() {
        assertNull(ResetPasswordFragment.validate("token", "Password1", "Password1"));
    }

    @Test
    public void rejectsMissingToken() {
        assertEquals("Nedostaje token iz email linka.",
                ResetPasswordFragment.validate(null, "Password1", "Password1"));
    }

    @Test
    public void rejectsMismatchedPasswords() {
        assertEquals("Lozinke se ne podudaraju.",
                ResetPasswordFragment.validate("token", "Password1", "Password2"));
    }
}
