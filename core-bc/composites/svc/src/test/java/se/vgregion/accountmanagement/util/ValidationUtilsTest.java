package se.vgregion.accountmanagement.util;

import org.junit.Test;
import se.vgregion.accountmanagement.ValidationException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Patrik Bergström
 */
public class ValidationUtilsTest {

    @Test
    public void testValidateEmail1() throws Exception {
        ValidationUtils.validateEmail("asdf@email.com", "asdf@email.com");
    }

    @Test(expected = ValidationException.class)
    public void testValidateEmail2() throws Exception {
        ValidationUtils.validateEmail("asdf@email.com", "asdföö@email.com");
    }

    @Test(expected = ValidationException.class)
    public void testValidateEmail3() throws Exception {
        ValidationUtils.validateEmail("asdfemail.com", "asdfemail.com");
    }

    @Test
    public void testValidatePassword1() throws Exception {
        ValidationUtils.validatePassword("asdf1234", "asdf1234");
    }

    @Test(expected = ValidationException.class)
    public void testValidatePassword2() throws Exception {
        ValidationUtils.validatePassword("asdf1343", "");
    }

    @Test(expected = ValidationException.class)
    public void testValidatePassword3() throws Exception {
        ValidationUtils.validatePassword("qwer33", "asrgea33");
    }

    @Test(expected = ValidationException.class)
    public void testValidatePassword4() throws Exception {
        ValidationUtils.validatePassword("a12", "a12");
    }

    @Test(expected = ValidationException.class)
    public void testValidatePassword5() throws Exception {
        ValidationUtils.validatePassword("aasdf123434¤", "aasdf123434¤");
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertTrue(ValidationUtils.isEmpty(null));
        assertTrue(ValidationUtils.isEmpty(""));
        assertFalse(ValidationUtils.isEmpty("asdf"));
    }

    @Test
    public void testIsEmail() throws Exception {
        assertTrue(ValidationUtils.isEmail("asdfa@asdkfj.com"));
        assertTrue(ValidationUtils.isEmail("as3....df_--a333@asd33kfj.com"));

        assertFalse(ValidationUtils.isEmail("asdfa(at)asdkfj.com"));
        assertFalse(ValidationUtils.isEmail("@asdkfj.com"));
    }
}
