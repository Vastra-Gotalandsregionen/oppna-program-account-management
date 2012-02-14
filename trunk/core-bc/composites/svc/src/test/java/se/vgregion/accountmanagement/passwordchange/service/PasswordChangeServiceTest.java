package se.vgregion.accountmanagement.passwordchange.service;

import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageBus;
import com.liferay.portal.kernel.messaging.MessageBusException;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.messaging.sender.MessageSender;
import com.liferay.portal.kernel.messaging.sender.SynchronousMessageSender;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.simple.SimpleLdapTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import se.vgregion.accountmanagement.PasswordChangeException;
import se.vgregion.accountmanagement.service.LdapAccountService;
import se.vgregion.accountmanagement.service.PasswordChangeService;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;
import se.vgregion.ldapservice.SimpleLdapUser;
import se.vgregion.portal.cs.domain.UserSiteCredential;
import se.vgregion.portal.cs.service.CredentialService;

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
    private LdapAccountService ldapAccountService = new LdapAccountService();

    @InjectMocks
    private PasswordChangeService passwordChangeService = new PasswordChangeService();

    /*@Value("${ldap.personnel.base}")
    private String base;
    */private final String changePasswordMessagebusDestination = "vgr/change_password";
    private final String verifyPasswordMessagebusDestination = "vgr/verify_password";

    @Before
    public void setup() {
        passwordChangeService.setLdapAccountService(ldapAccountService);
        
        ReflectionTestUtils.setField(passwordChangeService, "changePasswordMessagebusDestination",
                changePasswordMessagebusDestination);
        ReflectionTestUtils.setField(passwordChangeService, "verifyPasswordMessagebusDestination",
                verifyPasswordMessagebusDestination);
        when(credentialService.getUserSiteCredential(anyString(), eq("iNotes"))).thenReturn(new UserSiteCredential());
        PowerMockito.mockStatic(MessageBusUtil.class);
        MessageBusUtil.init(mock(MessageBus.class), mock(MessageSender.class), mock(SynchronousMessageSender.class));
        passwordChangeService.setLimit(3); //three seconds
        passwordChangeService.setDelay(400);

        Element element = new Element("asdf", "asdf", 1);
        ReflectionTestUtils.setField(element, "lastUpdateTime", System.currentTimeMillis());
        when(ehcache.get(anyString())).thenReturn(element);
    }

    @Test
    public void testUpdateDominoLdapAndInotes() throws MessageBusException, PasswordChangeException,
            NoSuchAlgorithmException, UnsupportedEncodingException, InterruptedException {
        final String screenName = "testScreenName";
        final String password = "userPassword";

        MessageDigest sha1Digest = MessageDigest.getInstance("SHA");
        byte[] digest = sha1Digest.digest(password.getBytes("UTF-8"));
        String encryptedPassword = "{SHA}" + DatatypeConverter.printBase64Binary(digest);

        setupUserInMockLdapService(screenName, encryptedPassword);
        when(MessageBusUtil.sendSynchronousMessage(eq(changePasswordMessagebusDestination), any(Message.class), anyInt()))
                .thenAnswer(new Answer<String>() {
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable {
                        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                "<response>\n" +
                                "  <statuscode>1</statuscode>\n" +
                                "  <statusmessage>Lösenordet uppdaterades för xxx</statusmessage>\n" +
                                "</response>";
                    }
                });
        when(MessageBusUtil.sendSynchronousMessage(eq(verifyPasswordMessagebusDestination), any(Message.class), anyInt()))
                .thenAnswer(new Answer<String>() {
                    //The first call replies that it HASN'T been updated yet (statusmessage = 0)
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable {
                        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                "<response>\n" +
                                "  <statuscode>1</statuscode>\n" +
                                "  <statusmessage>0</statusmessage>\n" +
                                "</response>";
                    }
                }).thenAnswer(new Answer<String>() {
                    //The second call replies that it HAS been updated (statusmessage = 1)
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable {
                        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                "<response>\n" +
                                "  <statuscode>1</statuscode>\n" +
                                "  <statusmessage>1</statusmessage>\n" +
                                "</response>";
                    }
                });


        passwordChangeService.updateDominoLdapAndInotes(screenName, password);

        Thread.sleep(1000);
        verify(ehcache).remove(screenName);
    }


    /*//Test the setPasswordInLdap method. It verifies that the password is correctly set but the ldap service is
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
    }*/

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
