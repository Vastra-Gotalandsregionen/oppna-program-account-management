package se.vgregion.accountmanagement.passwordchange.controller;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.messaging.MessageBus;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.messaging.sender.MessageSender;
import com.liferay.portal.kernel.messaging.sender.SynchronousMessageSender;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.theme.ThemeDisplay;
import junit.framework.TestCase;
import net.sf.ehcache.Ehcache;
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
import org.springframework.ui.Model;
import se.vgregion.accountmanagement.service.LdapAccountService;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;
import se.vgregion.ldapservice.SimpleLdapUser;
import se.vgregion.portal.cs.service.CredentialService;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;

import static org.mockito.Mockito.*;

/**
 * @author Patrik Bergström
 */
@PrepareForTest(MessageBusUtil.class)
@RunWith(MockitoJUnitRunner.class)
public class PasswordChangeControllerTest extends TestCase {


    @Mock
    private SimpleLdapServiceImpl simpleLdapService; //this is injected in the @InjectMocks-annotated instance
    @Mock
    private CredentialService credentialService;
    @Mock
    private Ehcache ehcache;

    @InjectMocks
    private LdapAccountService ldapAccountService = new LdapAccountService();
    
    @InjectMocks
    private PasswordChangeController controller = new PasswordChangeController();

    {
        controller.setLdapAccountService(ldapAccountService);
    }

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
    public void testShowSuccessPage() throws Exception {
        String view = controller.showSuccessPage();
        assertNotNull(view);
    }

    @Test
    public void testChangePasswordForUser() throws Exception {

        //initial setup
        ActionRequest request = prepareActionRequest();

        String userId = "ex_teste";
        MessageDigest sha = MessageDigest.getInstance("SHA");
        String newPassword = "123abc";

        byte[] digest = sha.digest(newPassword.getBytes("UTF-8"));
        String encryptedPassword = "{SHA}" + DatatypeConverter.printBase64Binary(digest);

        setupUserInMockLdapService(userId, encryptedPassword);

        //Given
        ActionResponse response = mock(ActionResponse.class);
        when(request.getParameter("password")).thenReturn(newPassword);
        when(request.getParameter("passwordConfirm")).thenReturn(newPassword);

        //Do
        controller.changePassword(request, response, mock(Model.class));

        //Verify
        verify(response).setRenderParameter(eq("success"), anyString());
    }

    private ActionRequest prepareActionRequest() throws PortalException, SystemException {
        ActionRequest request = mock(ActionRequest.class);
        ThemeDisplay themeDisplay = new ThemeDisplay();
        User user = mock(User.class);
        when(user.getScreenName()).thenReturn("ex_teste");
        themeDisplay.setUser(user);
        when(request.getAttribute(WebKeys.THEME_DISPLAY)).thenReturn(themeDisplay);

        return request;
    }

    //This test verifies that "failure" is set on the response when no reply is returned from the messageBus.
    @Test
    public void testChangePasswordNoReply() throws Exception {

        //initial setup
        ActionRequest request = prepareActionRequest();

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

        ActionResponse response = mock(ActionResponse.class);
        when(request.getParameter("password")).thenReturn("123abc");
        when(request.getParameter("passwordConfirm")).thenReturn("123abc");
        Model model = mock(Model.class);

        //Do
        controller.changePassword(request, response, model);

        //Verify
        verify(model).addAttribute(eq("errorMessage"), anyString());
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

        ActionResponse response = mock(ActionResponse.class);

        when(request.getParameter("password")).thenReturn("123abc");
        when(request.getParameter("passwordConfirm")).thenReturn("anotherPassword"); //The important thing with this test

        //Do
        Model model = mock(Model.class);
        controller.changePassword(request, response, model);

        //Verify
        verify(model).addAttribute(eq("errorMessage"), anyString());
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
