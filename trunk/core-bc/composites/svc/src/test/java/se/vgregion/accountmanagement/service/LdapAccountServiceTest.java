package se.vgregion.accountmanagement.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.simple.SimpleLdapTemplate;
import se.vgregion.accountmanagement.LdapException;
import se.vgregion.accountmanagement.PasswordChangeException;
import se.vgregion.ldapservice.LdapUser;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;
import se.vgregion.ldapservice.SimpleLdapUser;

import javax.naming.NamingException;
import javax.naming.directory.ModificationItem;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Patrik Bergström
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapAccountServiceTest {

    @Mock
    private SimpleLdapServiceImpl simpleLdapService; //this is injected in the @InjectMocks-annotated instance

    @InjectMocks
    private LdapAccountService ldapAccountService = new LdapAccountService();

    //Test the setPasswordInLdap method. It verifies that the password is correctly set but the ldap service is
    //mocked so it does not test it all the way.
    @Test
    public void testSetPasswordInLdap() throws NoSuchAlgorithmException, UnsupportedEncodingException,
            NamingException, PasswordChangeException {

        String userId = "ex_teste";
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA");
        String newPassword = "test";// + new Random().nextInt(100);

        byte[] digest = sha1Digest.digest(newPassword.getBytes("UTF-8"));
        String encryptedPassword = "{SHA}" + DatatypeConverter.printBase64Binary(digest);

        setupUserInMockLdapService(userId, encryptedPassword);

        //do it
        ldapAccountService.setPasswordInLdap(userId, newPassword);

        //verify
        byte[] digest2 = sha1Digest.digest(newPassword.getBytes("UTF-8"));
        String expecedEncryptedPassword = "{SHA}" + DatatypeConverter.printBase64Binary(digest2);

        SimpleLdapUser ldapUser = (SimpleLdapUser) simpleLdapService.getLdapUserByUid("anyString", "ex_teste");
        byte[] userPassword = (byte[]) ldapUser.getAttributes(new String[]{"userPassword"}).get("userPassword").get();
        String encryptedPassword2 = new String(userPassword, "UTF-8");

        assertEquals(expecedEncryptedPassword, encryptedPassword2);
    }

    @Test
    public void testSetAttributes() throws Exception {

        // Given
        LdapUser ldapUser = mock(LdapUser.class);
        when(simpleLdapService.getLdapUserByUid("ou=externa,ou=anv,o=VGR", "thisId")).thenReturn(ldapUser);

        LdapOperations ldapOperations = setupLdapTemplateOnLdapService(simpleLdapService);
        
        // Create HashMap with three entries
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("1", "1");
        attributes.put("2", "2");
        attributes.put("3", "3");

        // When
        ldapAccountService.setAttributes("thisId", attributes);

        // The ArgumentCaptor will fetch an argument which is sent to a method.
        ArgumentCaptor<ModificationItem[]> argumentCaptor = ArgumentCaptor.forClass(ModificationItem[].class);

        // Then (we test that the modificationItems have the same number as the attributes map)
        verify(ldapOperations).modifyAttributes(anyString(), argumentCaptor.capture());
        ModificationItem[] modificationItems = argumentCaptor.getValue();
        assertEquals(3, modificationItems.length); // Corresponds to the number of the attributes map
    }

    private LdapOperations setupLdapTemplateOnLdapService(SimpleLdapServiceImpl simpleLdapService) {
        LdapOperations ldapOperations = mock(LdapOperations.class);

        SimpleLdapTemplate simpleLdapTemplate = mock(SimpleLdapTemplate.class);

        when(simpleLdapTemplate.getLdapOperations()).thenReturn(ldapOperations);
        when(simpleLdapService.getLdapTemplate()).thenReturn(simpleLdapTemplate);
        return ldapOperations;
    }

    @Test
    public void testSetEmailInLdap() throws LdapException, NamingException {

        // Given
        LdapUser ldapUser = mock(LdapUser.class);
        LdapOperations ldapOperations = setupLdapTemplateOnLdapService(simpleLdapService);

        when(simpleLdapService.getLdapUserByUid((String) isNull(), eq("thisId"))).thenReturn(ldapUser);

        String email = "test@email.com";

        // When
        ldapAccountService.setEmailInLdap("thisId",  email);

        // The ArgumentCaptor will fetch an argument which is sent to a method.
        ArgumentCaptor<ModificationItem[]> argumentCaptor = ArgumentCaptor.forClass(ModificationItem[].class);

        // Then (we test that the email value is set correctly)
        verify(ldapOperations).modifyAttributes(anyString(), argumentCaptor.capture());
        ModificationItem[] modificationItems = argumentCaptor.getValue();
        assertEquals(1, modificationItems.length); // Only one attribute should be set (the "mail" attribute)
        assertEquals(email, modificationItems[0].getAttribute().get()); // It should equal the email String
    }

    private void setupUserInMockLdapService(String userId, String encryptedPassword) {
        SimpleLdapUser user = new SimpleLdapUser("anyString");
        user.setAttributeValue("userPassword", encryptedPassword.getBytes());
        when(simpleLdapService.getLdapUserByUid(anyString(), eq(userId))).thenReturn(user);
        SimpleLdapTemplate simpleLdapTemplate = mock(SimpleLdapTemplate.class);
        LdapOperations ldapOperations = mock(LdapOperations.class);
        when(simpleLdapTemplate.getLdapOperations()).thenReturn(ldapOperations);
        when(simpleLdapService.getLdapTemplate()).thenReturn(simpleLdapTemplate);
    }

}
