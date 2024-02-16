package io.devjoy.gitea.util;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PasswordServiceTest {

    PasswordService service = new PasswordService();

    @Test
    void testLengthTen() {
       String pw = service.generateNewPassword(10);
       assertEquals(10, pw.length());
    }

    @Test
    void upperCase() {
        String upperCaseLetters = service.random(2, 65, 90);
        assertEquals(upperCaseLetters.toUpperCase(), upperCaseLetters);
        assertEquals(2, upperCaseLetters.length());
    }

    @Test
    void lowerCase() {
	    String lowerCaseLetters = service.random(2, 97, 122);
        assertEquals(lowerCaseLetters.toLowerCase(), lowerCaseLetters);
        assertEquals(2, lowerCaseLetters.length());
    }

    @Test
    void numbers() {
	    String numbers = service.randomNumeric(2);
        Integer.parseInt(numbers);
        assertEquals(2, numbers.length());
    }

    @Test
    void specialChar() {
	    String specials = service.random(2, 33, 47);
        assertEquals(2, specials.length());
    }

    @Test
    void alphanumeric() {
	    String specials =  service.randomAlphanumeric(2);
        assertEquals(2, specials.length());
    }
   
}