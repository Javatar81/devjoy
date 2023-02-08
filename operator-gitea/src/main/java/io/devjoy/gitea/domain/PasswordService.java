package io.devjoy.gitea.domain;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.Min;

import org.apache.commons.lang3.RandomStringUtils;

@ApplicationScoped
public class PasswordService {
	
	public String generateNewPassword(@Min(10) int length) {
		String upperCaseLetters = RandomStringUtils.random(2, 65, 90, true, true);
	    String lowerCaseLetters = RandomStringUtils.random(2, 97, 122, true, true);
	    String numbers = RandomStringUtils.randomNumeric(2);
	    String specialChar = RandomStringUtils.random(2, 33, 47, false, false);
	    String totalChars = RandomStringUtils.randomAlphanumeric(length - 8);
	    String combinedChars = upperCaseLetters.concat(lowerCaseLetters)
	      .concat(numbers)
	      .concat(specialChar)
	      .concat(totalChars);
	    List<Character> pwdChars = combinedChars.chars()
	      .mapToObj(c -> (char) c)
	      .collect(Collectors.toList());
	    Collections.shuffle(pwdChars);
	    return pwdChars.stream()
	      .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
	      .toString();
	}
}
