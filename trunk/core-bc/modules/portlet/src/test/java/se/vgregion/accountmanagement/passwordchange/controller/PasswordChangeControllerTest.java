package se.vgregion.accountmanagement.passwordchange.controller;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.messaging.MessageBus;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.messaging.sender.MessageSender;
import com.liferay.portal.kernel.messaging.sender.SynchronousMessageSender;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.theme.ThemeDisplay;
import junit.framework.TestCase;
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
import org.springframework.ui.Model;
import se.vgregion.accountmanagement.passwordchange.PasswordChangeException;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;
import se.vgregion.ldapservice.SimpleLdapUser;

import javax.naming.NamingException;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

import static org.mockito.Mockito.*;

/**
 * @author Patrik Bergström
 */
@PrepareForTest(MessageBusUtil.class)
@RunWith(MockitoJUnitRunner.class)
public class PasswordChangeControllerTest extends TestCase {

    @Mock
    private SimpleLdapServiceImpl simpleLdapService;

    @InjectMocks
    private PasswordChangeController controller = new PasswordChangeController();

    //A method counts as a method in the test coverage statistics ;)
    @Test
    public void testShowPasswordChangeForm() throws Exception {
        //Given
        RenderRequest request = mock(RenderRequest.class);
        ThemeDisplay themeDisplay = new ThemeDisplay();
        User user = mock(User.class);
        themeDisplay.setUser(user);
        when(request.getAttribute(WebKeys.THEME_DISPLAY)).thenReturn(themeDisplay);

        //Go
        String view = controller.showPasswordChangeForm(request, mock(Model.class));

        //Verify
        assertNotNull(view);
    }

    @Test
    public void testShowPasswordChangeFormWithError() throws Exception {
        //Given
        RenderRequest request = mock(RenderRequest.class);
        ThemeDisplay themeDisplay = new ThemeDisplay();
        User user = mock(User.class);
        themeDisplay.setUser(user);
        when(request.getAttribute(WebKeys.THEME_DISPLAY)).thenReturn(themeDisplay);
        String errorMessage = "Ett felmeddelande.";
        Model model = mock(Model.class);

        //do
        String view = controller.showPasswordChangeFormWithError(errorMessage, request, model);

        //verify
        verify(model).addAttribute(eq("errorMessage"), eq(errorMessage));

    }

    @Test
    public void testShowSuccessPage() throws Exception {
        String view = controller.showSuccessPage();
        assertNotNull(view);
    }

    //This tests the flow of the changePassword method and verifies that "success" is set on the response. It does
    //not verify that the password is set.
    @Test
    public void testChangePassword() throws Exception {

        //initial setup
        ActionRequest request = prepareForIsDominoUserMethod(false);
        ThemeDisplay themeDisplay = new ThemeDisplay();
        User user = mock(User.class);
        when(user.getScreenName()).thenReturn("ex_teste");
        themeDisplay.setUser(user);
        when(request.getAttribute(WebKeys.THEME_DISPLAY)).thenReturn(themeDisplay);

        String userId = "ex_teste";
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        String newPassword = "123abc";

        byte[] digest = md5.digest(newPassword.getBytes("UTF-8"));
        String encryptedPassword = "{MD5}" + DatatypeConverter.printBase64Binary(digest);

        setupUserInMockLdapService(userId, encryptedPassword);

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
        when(request.getParameter("password")).thenReturn(newPassword);
        when(request.getParameter("passwordConfirm")).thenReturn(newPassword);

        //Do
        controller.changePassword(request, response);

        //Verify
        verify(response).setRenderParameter(eq("success"), anyString());
    }

    private ActionRequest prepareForIsDominoUserMethod(boolean isDomino) throws PortalException, SystemException {
        ActionRequest request = mock(ActionRequest.class);
        ThemeDisplay themeDisplay = new ThemeDisplay();
        User user = mock(User.class);
        themeDisplay.setUser(user);
        when(request.getAttribute(WebKeys.THEME_DISPLAY)).thenReturn(themeDisplay);

        //add a role
        if (isDomino) {
            ArrayList<Role> roles = new ArrayList<Role>();
            Role role1 = mock(Role.class);
            when(role1.getTitle()).thenReturn("Domino");
            roles.add(role1);
            when(user.getRoles()).thenReturn(roles);
        }
        return request;
    }

    //This test verifies that "failure" is set on hte response when no reply is returned from the messageBus.
    @Test
    public void testChangePasswordNoReply() throws Exception {

        //initial setup
        ActionRequest request = prepareForIsDominoUserMethod(true);

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
        when(request.getParameter("password")).thenReturn("123abc");
        when(request.getParameter("passwordConfirm")).thenReturn("123abc");

        //Do
        controller.changePassword(request, response);

        //Verify
        verify(response).setRenderParameter(eq("failure"), anyString());
    }

    //This tests the flow when two non-equal passwords are entered. A failure should result.
    @Test
    public void testChangePasswordPasswordValidateError() throws Exception {

        //Given
        //initial setup
        ActionRequest request = mock(ActionRequest.class);
        ThemeDisplay themeDisplay = new ThemeDisplay();
        User user = mock(User.class);
        themeDisplay.setUser(user);
        when(request.getAttribute(WebKeys.THEME_DISPLAY)).thenReturn(themeDisplay);

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

        when(request.getParameter("password")).thenReturn("123abc");
        when(request.getParameter("passwordConfirm")).thenReturn("anotherPassword"); //The important thing with this test

        //Do
        controller.changePassword(request, response);

        //Verify
        verify(response).setRenderParameter(eq("failure"), anyString());
    }

    //Test the isDominoUser method.
    @Test
    public void testIsDominoUser() throws PasswordChangeException, SystemException, PortalException {
        //initial setup
        ActionRequest request = mock(ActionRequest.class);
        ThemeDisplay themeDisplay = new ThemeDisplay();
        User user = mock(User.class);
        themeDisplay.setUser(user);
        when(request.getAttribute(WebKeys.THEME_DISPLAY)).thenReturn(themeDisplay);

        //first try with no roles
        assertFalse(controller.isDominoUser(request));

        //add a role
        ArrayList<Role> roles = new ArrayList<Role>();
        Role role1 = mock(Role.class);
        when(role1.getTitle()).thenReturn("someArbitraryTitle");
        roles.add(role1);
        when(user.getRoles()).thenReturn(roles);

        //now try with a role that isn't domino
        assertFalse(controller.isDominoUser(request));

        //now add a domino role
        Role role2 = mock(Role.class);
        when(role2.getTitle()).thenReturn("DominoRole");
        roles.add(role2);

        //verify we get "true" back
        assertTrue(controller.isDominoUser(request));
    }

    //Test the setPasswordInLdap method. It verifies that the password is correctly set but the ldap service is
    //mocked so it does not test it all the way.
    @Test
    public void testSetPasswordInLdap() throws NoSuchAlgorithmException, UnsupportedEncodingException,
            NamingException, PasswordChangeException {

        String userId = "ex_teste";
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        String newPassword = "test" + new Random().nextInt(100);

        byte[] digest = md5.digest(newPassword.getBytes("UTF-8"));
        String encryptedPassword = "{MD5}" + DatatypeConverter.printBase64Binary(digest);

        setupUserInMockLdapService(userId, encryptedPassword);

        //do it
        controller.setPasswordInLdap(userId, newPassword);

        //verify
        byte[] digest2 = md5.digest(newPassword.getBytes("UTF-8"));
        String expecedEncryptedPassword = "{MD5}" + DatatypeConverter.printBase64Binary(digest2);

        SimpleLdapUser ldapUser = (SimpleLdapUser) simpleLdapService.getLdapUserByUid("ex_teste");
        byte[] userPassword = (byte[]) ldapUser.getAttributes(new String[]{"userPassword"}).get("userPassword").get();
        String encryptedPassword2 = new String(userPassword, "UTF-8");

        assertEquals(expecedEncryptedPassword, encryptedPassword2);
    }

    private void setupUserInMockLdapService(String userId, String encryptedPassword) {
        SimpleLdapUser user = new SimpleLdapUser("anyString");
        user.setAttributeValue("userPassword", encryptedPassword.getBytes());
        when(simpleLdapService.getLdapUserByUid(userId)).thenReturn(user);
        SimpleLdapTemplate simpleLdapTemplate = mock(SimpleLdapTemplate.class);
        LdapOperations ldapOperations = mock(LdapOperations.class);
        when(simpleLdapTemplate.getLdapOperations()).thenReturn(ldapOperations);
        when(simpleLdapService.getLdapTemplate()).thenReturn(simpleLdapTemplate);
    }
}