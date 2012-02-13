package se.vgregion.accountmanagement.passwordchange.controller;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.messaging.MessageBus;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.messaging.sender.MessageSender;
import com.liferay.portal.kernel.messaging.sender.SynchronousMessageSender;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroup;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;
import se.vgregion.accountmanagement.passwordchange.PasswordChangeException;
import se.vgregion.accountmanagement.service.LdapAccountService;
import se.vgregion.accountmanagement.service.PasswordChangeService;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;
import se.vgregion.ldapservice.SimpleLdapUser;
import se.vgregion.portal.cs.service.CredentialService;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private PasswordChangeService passwordChangeService = new PasswordChangeService();

    @InjectMocks
    private PasswordChangeController controller = new PasswordChangeController();

    /*@Value("${ldap.personnel.base}")
    private String base;

    */private String dominoUsersUserGroupName = "DominoUsers";

    {
        passwordChangeService.setLdapAccountService(ldapAccountService);
        controller.setLdapAccountService(ldapAccountService);
        controller.setDominoUsersUserGroupName(dominoUsersUserGroupName);
        controller.setPasswordChangeService(passwordChangeService);
    }

    /*@Before
    public void setup() {
        when(credentialService.getUserSiteCredential(anyString(), eq("iNotes"))).thenReturn(new UserSiteCredential());
    }

    */
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

    //This tests the flow of the changePassword method and verifies that "success" is set on the response. It does
    //not verify that the password is set.
    @Test
    public void testChangePasswordDominoUser() throws Exception {

        //initial setup
        ActionRequest request = prepareForIsDominoUserMethod(true);

        String userId = "ex_teste";
        MessageDigest sha = MessageDigest.getInstance("SHA");
        String newPassword = "123abc";

        byte[] digest = sha.digest(newPassword.getBytes("UTF-8"));
        String encryptedPassword = "{SHA}" + DatatypeConverter.printBase64Binary(digest);

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
                        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                "<response>\n" +
                                "<statuscode>1</statuscode>\n" +
                                "<statusmessage>Lösenordet uppdaterades för xxtst1</statusmessage>\n" +
                                "</response>";
                    }
                });
        ReflectionTestUtils.setField(passwordChangeService, "changePasswordMessagebusDestination", messagebusDestination);
        ActionResponse response = mock(ActionResponse.class);
        when(request.getParameter("password")).thenReturn(newPassword);
        when(request.getParameter("passwordConfirm")).thenReturn(newPassword);

        //Do
        controller.changePassword(request, response, mock(Model.class));

        //Verify
        verify(response).setRenderParameter(eq("success"), anyString());
//        verify(credentialService).save(any(UserSiteCredential.class));
    }

    @Test
    public void testChangePasswordNonDominoUser() throws Exception {

        //initial setup
        ActionRequest request = prepareForIsDominoUserMethod(false);

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
//        verify(credentialService).save(any(UserSiteCredential.class));
    }

    private ActionRequest prepareForIsDominoUserMethod(boolean isDomino) throws PortalException, SystemException {
        ActionRequest request = mock(ActionRequest.class);
        ThemeDisplay themeDisplay = new ThemeDisplay();
        User user = mock(User.class);
        when(user.getScreenName()).thenReturn("ex_teste");
        themeDisplay.setUser(user);
        when(request.getAttribute(WebKeys.THEME_DISPLAY)).thenReturn(themeDisplay);

        //add a role
        if (isDomino) {
            UserGroup userGroup = mock(UserGroup.class);
            when(userGroup.getName()).thenReturn("DominoUsers");
            List<UserGroup> userGroups = Arrays.asList(userGroup);
            when(user.getUserGroups()).thenReturn(userGroups);
        }
        return request;
    }

    //This test verifies that "failure" is set on the response when no reply is returned from the messageBus.
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
        ReflectionTestUtils.setField(passwordChangeService, "changePasswordMessagebusDestination", messagebusDestination);
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
        ReflectionTestUtils.setField(passwordChangeService, "changePasswordMessagebusDestination", messagebusDestination);
        ActionResponse response = mock(ActionResponse.class);

        when(request.getParameter("password")).thenReturn("123abc");
        when(request.getParameter("passwordConfirm")).thenReturn("anotherPassword"); //The important thing with this test

        //Do
        Model model = mock(Model.class);
        controller.changePassword(request, response, model);

        //Verify
        verify(model).addAttribute(eq("errorMessage"), anyString());
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
        List<UserGroup> userGroups = new ArrayList<UserGroup>();
        UserGroup userGroup1 = mock(UserGroup.class);
        when(userGroup1.getName()).thenReturn("someArbitraryName");
        when(user.getUserGroups()).thenReturn(userGroups);

        //now try with a role that isn't domino
        assertFalse(controller.isDominoUser(request));

        //now add a domino role
        UserGroup userGroup2 = mock(UserGroup.class);
        when(userGroup2.getName()).thenReturn(dominoUsersUserGroupName);
        userGroups.add(userGroup2);

        //verify we get "true" back
        assertTrue(controller.isDominoUser(request));
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
