package se.vgregion.accountmanagement.util;

import se.vgregion.accountmanagement.ValidationException;

import java.util.regex.Pattern;

/**
 * Utility class with static methods for validation of different concepts.
 *
 * @author Patrik Bergström
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // Private constructor in utility class
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(".+@.+\\.[a-z]+");

    /**
     * Validates that two arguments are not empty, equal and valid email addresses.
     *
     * @param email email argument one
     * @param confirmEmail email argument two
     * @throws ValidationException if some of the criteria fails
     */
    public static void validateEmail(String email, String confirmEmail) throws ValidationException {
        if (isEmpty(email) || isEmpty(confirmEmail)) {
            throw new ValidationException("Fyll i båda fälten.");
        }

        if (!email.equals(confirmEmail)) {
            throw new ValidationException("Båda fälten måste vara lika.");
        }

        if (!isEmail(email)) {
            throw new ValidationException("Ogiltig e-postadress.");
        }
    }


    /**
     * Validates that two arguments are not empty, equal and have the right strength.
     *
     * @param password password argument one
     * @param confirmPassword password argument two
     * @throws ValidationException if some of the criteria fails
     */
    public static void validatePassword(String password, String confirmPassword) throws ValidationException {
        if (isEmpty(password) || isEmpty(confirmPassword)) {
            throw new ValidationException("Fyll i båda fälten.");
        }

        if (!password.equals(confirmPassword)) {
            throw new ValidationException("Båda fälten måste vara lika.");
        }

        final int minLength = 6;
        if (password.length() < minLength) {
            throw new ValidationException("Lösenord måste vara minst 6 tecken.");
        }

        if (!password.matches("[a-zA-Z0-9]*")) {
            throw new ValidationException("Lösenordet får bara innehålla bokstäver och siffror");
        }

        if (!(password.matches(".*[a-zA-Z]+.*") && password.matches(".*[0-9]+.*"))) {
            throw new ValidationException("Lösenordet måste innehålla både bokstäver och siffror");
        }
    }

    /**
     * Tests whether a String is null or zero-length.
     *
     * @param s the String to test
     * @return <code>true</code> if the String is null or zero-length or <code>false</code> otherwise
     */
    public static boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }

    /**
     * Tests whether a String is a valid email address.
     *
     * @param value the String to test
     * @return <code>true</code>
     */
    public static boolean isEmail(String value) {
        return EMAIL_PATTERN.matcher(value).matches();
    }

}
