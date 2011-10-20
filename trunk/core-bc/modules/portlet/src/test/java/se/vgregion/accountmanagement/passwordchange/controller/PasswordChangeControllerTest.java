package se.vgregion.accountmanagement.passwordchange.controller;

import com.liferay.portal.kernel.messaging.MessageBus;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.messaging.sender.MessageSender;
import com.liferay.portal.kernel.messaging.sender.SynchronousMessageSender;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.simple.ParameterizedContextMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;
import se.vgregion.ldapservice.SimpleLdapUser;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static org.mockito.Mockito.*;

/**
 * @author Patrik Bergström
 */
@PrepareForTest(MessageBusUtil.class)
@RunWith(MockitoJUnitRunner.class)
//@ContextConfiguration({"classpath:applicationContext-test.xml}"})
public class PasswordChangeControllerTest extends TestCase {

    private PasswordChangeController controller;// = new PasswordChangeController();

    private SimpleLdapServiceImpl simpleLdapService;

    @Before
    public void setup() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext-test.xml");
        controller = ctx.getBean(PasswordChangeController.class);
        simpleLdapService = (SimpleLdapServiceImpl) ctx.getBean("simpleLdapService");
    }

    //A method counts as a method in the test coverage statistics ;)
    @Test
    public void testShowPasswordChangeForm() throws Exception {
        String view = controller.showPasswordChangeForm();
        assertNotNull(view);
    }

    @Test
    public void testShowPasswordChangeFormWithError() throws Exception {
        String errorMessage = "Ett felmeddelande.";
        Model model = mock(Model.class);
        String view = controller.showPasswordChangeFormWithError(errorMessage, model);

        verify(model).addAttribute(eq("errorMessage"), eq(errorMessage));

    }

    @Test
    public void testShowSuccessPage() throws Exception {
        String view = controller.showSuccessPage();
        assertNotNull(view);
    }

    @Test
    public void testChangePassword() throws Exception {

        //Given
        PowerMockito.mockStatic(MessageBusUtil.class);
        MessageBusUtil.init(mock(MessageBus.class), mock(MessageSender.class), mock(SynchronousMessageSender.class));
        String messagebusDestination = "vgr/change_password";
        when(MessageBusUtil.sendSynchronousMessage(eq(messagebusDestination),
                any(com.liferay.portal.kernel.messaging.Message.class), anyInt()))
                .thenAnswer(new Answer<String>() { //Answer is chosen since you can throw exceptions
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable {
                        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                                "<xmlResponse>Test</xmlResponse>";
                    }
                });
        ReflectionTestUtils.setField(controller, "messagebusDestination", messagebusDestination);
        ActionResponse response = mock(ActionResponse.class);
        ActionRequest request = mock(ActionRequest.class);
        when(request.getParameter("password")).thenReturn("123abc");
        when(request.getParameter("passwordConfirm")).thenReturn("123abc");

        //Do
        controller.changePassword(request, response);

        //Verify
        verify(response).setRenderParameter(eq("success"), anyString());
    }

    @Test
    public void testChangePasswordNoReply() throws Exception {

        //Given
        PowerMockito.mockStatic(MessageBusUtil.class);
        MessageBusUtil.init(mock(MessageBus.class), mock(MessageSender.class), mock(SynchronousMessageSender.class));
        String messagebusDestination = "vgr/change_password";
        when(MessageBusUtil.sendSynchronousMessage(eq(messagebusDestination),
                any(com.liferay.portal.kernel.messaging.Message.class), anyInt()))
                .thenAnswer(new Answer<String>() { //Answer is chosen since you can throw exceptions
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable {
                        return null;
                    }
                });
        ReflectionTestUtils.setField(controller, "messagebusDestination", messagebusDestination);
        ActionResponse response = mock(ActionResponse.class);
        ActionRequest request = mock(ActionRequest.class);
        when(request.getParameter("password")).thenReturn("123abc");
        when(request.getParameter("passwordConfirm")).thenReturn("123abc");

        //Do
        controller.changePassword(request, response);

        //Verify
        verify(response).setRenderParameter(eq("failure"), anyString());
    }

    @Test
    public void testChangePasswordPasswordValidateError() throws Exception {

        //Given
        PowerMockito.mockStatic(MessageBusUtil.class);
        MessageBusUtil.init(mock(MessageBus.class), mock(MessageSender.class), mock(SynchronousMessageSender.class));
        String messagebusDestination = "vgr/change_password";
        when(MessageBusUtil.sendSynchronousMessage(eq(messagebusDestination),
                any(com.liferay.portal.kernel.messaging.Message.class), anyInt()))
                .thenAnswer(new Answer<String>() { //Answer is chosen since you can throw exceptions
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable {
                        return null;
                    }
                });
        ReflectionTestUtils.setField(controller, "messagebusDestination", messagebusDestination);
        ActionResponse response = mock(ActionResponse.class);
        ActionRequest request = mock(ActionRequest.class);
        when(request.getParameter("password")).thenReturn("123abc");
        when(request.getParameter("passwordConfirm")).thenReturn("anotherPassword");

        //Do
        controller.changePassword(request, response);

        //Verify
        verify(response).setRenderParameter(eq("failure"), anyString());
    }

    @Test
    public void testSetPasswordInLdap() throws NoSuchAlgorithmException, UnsupportedEncodingException, NamingException {
        SimpleLdapUser ldapUser = (SimpleLdapUser) simpleLdapService.getLdapUserByUid("ex_teste");

        assertNotNull(ldapUser);

        ldapUser.getAttributes();

        String newPassword = "test" + new Random().nextInt(100);
        System.out.println("New password = " + newPassword);

        controller.setPasswordInLdap("ex_teste", newPassword);

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digest = md5.digest(newPassword.getBytes("UTF-8"));
        String expecedEncryptedPassword = "{MD5}" + DatatypeConverter.printBase64Binary(digest);

        ldapUser = (SimpleLdapUser) simpleLdapService.getLdapUserByUid("ex_teste");
        byte[] userPassword = (byte[]) ldapUser.getAttributes(new String[]{"userPassword"}).get("userPassword").get();
        String encryptedPassword = new String(userPassword, "UTF-8");

        assertEquals(expecedEncryptedPassword, encryptedPassword);
    }
}
