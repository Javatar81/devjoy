package io.devjoy.gitea.domain;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.Min;

@ApplicationScoped
public class PasswordService {
	
	private final SecureRandom rand = new SecureRandom();

	String random(int length, int start, int end) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < length; i++) {
			builder.append((char) (rand.nextInt(end - start) +  start));
		}
		return builder.toString();
	}

	String randomNumeric(int length) {
		return rand.ints(length,0,9).mapToObj(String::valueOf).collect(Collectors.joining(""));
	}

	String randomAlphanumeric(int length) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0 ; i < length; i++) {
			if (rand.nextBoolean()) {
				builder.append(random(1, 65, 90));
			} else {
				builder.append(random(1, 97, 122));
			}
		}
		return builder.toString();
	}

	public String generateNewPassword(@Min(10) int length) {
		String upperCaseLetters = random(2, 65, 90);
	    String lowerCaseLetters = random(2, 97, 122);
	    String numbers = randomNumeric(2);
	    String specialChar = random(2, 33, 47);
	    String totalChars = randomAlphanumeric(length - 8);
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
