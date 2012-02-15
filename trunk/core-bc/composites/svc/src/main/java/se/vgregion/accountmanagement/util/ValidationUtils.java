package se.vgregion.accountmanagement.util;

import se.vgregion.accountmanagement.ValidationException;

import java.util.regex.Pattern;

/**
 * @author Patrik Bergström
 */
public class ValidationUtils {

    private static final Pattern emailPattern = Pattern.compile(".+@.+\\.[a-z]+");

    public static void validateEmail(String newEmail, String confirmEmail) throws ValidationException {
        if (isEmpty(newEmail) || isEmpty(confirmEmail)) {
            throw new ValidationException("Fyll i båda fälten.");
        }

        if (!newEmail.equals(confirmEmail)) {
            throw new ValidationException("Båda fälten måste vara lika.");
        }

        if (!isEmail(newEmail)) {
            throw new ValidationException("Ogiltig e-postadress.");
        }
    }
    
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

    public static boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }

    public static boolean isEmail(String value) {
        return emailPattern.matcher(value).matches();
    }

}
