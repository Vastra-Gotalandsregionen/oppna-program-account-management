package se.vgregion.accountmanagement.passwordchange.service;

import com.liferay.portal.kernel.messaging.MessageBusUtil;
import net.sf.ehcache.Ehcache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.simple.SimpleLdapTemplate;
import se.vgregion.accountmanagement.passwordchange.PasswordChangeException;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;
import se.vgregion.ldapservice.SimpleLdapUser;
import se.vgregion.portal.cs.domain.UserSiteCredential;
import se.vgregion.portal.cs.service.CredentialService;

import javax.naming.NamingException;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@PrepareForTest(MessageBusUtil.class)
@RunWith(MockitoJUnitRunner.class)
public class PasswordChangeServiceTest {

    @Mock
    private SimpleLdapServiceImpl simpleLdapService; //this is injected in the @InjectMocks-annotated instance
    @Mock
    private CredentialService credentialService;
    @Mock
    private Ehcache ehcache;

    @InjectMocks
    private PasswordChangeService passwordChangeService = new PasswordChangeService();

    @Value("${ldap.personnel.base}")
    private String base;

    @Before
    public void setup() {
        when(credentialService.getUserSiteCredential(anyString(), eq("iNotes"))).thenReturn(new UserSiteCredential());
    }



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
        passwordChangeService.setPasswordInLdap(userId, newPassword);

        //verify
        byte[] digest2 = sha1Digest.digest(newPassword.getBytes("UTF-8"));
        String expecedEncryptedPassword = "{SHA}" + DatatypeConverter.printBase64Binary(digest2);

        SimpleLdapUser ldapUser = (SimpleLdapUser) simpleLdapService.getLdapUserByUid(base, "ex_teste");
        byte[] userPassword = (byte[]) ldapUser.getAttributes(new String[]{"userPassword"}).get("userPassword").get();
        String encryptedPassword2 = new String(userPassword, "UTF-8");

        assertEquals(expecedEncryptedPassword, encryptedPassword2);
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
