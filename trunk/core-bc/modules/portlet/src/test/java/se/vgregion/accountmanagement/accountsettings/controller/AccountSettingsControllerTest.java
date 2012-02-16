package se.vgregion.accountmanagement.accountsettings.controller;

import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ui.Model;
import org.springframework.web.portlet.ModelAndView;
import se.vgregion.accountmanagement.service.LdapAccountService;
import se.vgregion.accountmanagement.service.LiferayAccountService;
import se.vgregion.ldapservice.LdapUser;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Patrik Bergström
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountSettingsControllerTest {

    @Mock
    private SimpleLdapServiceImpl simpleLdapService; //this is injected in the @InjectMocks-annotated instance
    @Mock
    private LdapAccountService ldapAccountService;
    @Mock
    private LiferayAccountService liferayAccountService;

    @InjectMocks
    private AccountSettingsController accountSettingsController = new AccountSettingsController();

    @Before
    public void setup() {
        accountSettingsController.setLdapAccountService(ldapAccountService);

    }

    @Test
    public void testShowAccountSettingsForm() throws Exception {

        // Given
        LdapUser ldapUser = mock(LdapUser.class);
        when(ldapUser.getAttributeValue("givenName")).thenReturn("testFirstName");
        when(ldapUser.getAttributeValue("middleName")).thenReturn("testMiddleName");
        when(ldapUser.getAttributeValue("sn")).thenReturn("testLastName");
        when(ldapUser.getAttributeValue("mail")).thenReturn("test@email.com");
        when(ldapUser.getAttributeValue("telephoneNumber")).thenReturn("065-46540684");
        when(ldapUser.getAttributeValue("mobile")).thenReturn("0754-6546545");
        when(ldapUser.getAttributeValue("externalStructurepersonDN")).thenReturn("testCompany/testDivision");

        when(ldapAccountService.getUser(anyString())).thenReturn(ldapUser);

        Model model = mock(Model.class);

        // When
        accountSettingsController.showAccountSettingsForm(mock(RenderRequest.class), mock(RenderResponse.class),
                model);

        // Then
        verify(model, times(7)).addAttribute(anyString(), anyString());
    }

    @Test
    public void testSaveGeneral() throws Exception {

        // Given
        ActionRequest request = mock(ActionRequest.class);
        String liferayUserId = "10000";
        when(request.getRemoteUser()).thenReturn(liferayUserId);
        User user = mock(User.class);
        when(liferayAccountService.lookupUser(Long.parseLong(liferayUserId))).thenReturn(user);

        // When
        ActionResponse response = mock(ActionResponse.class);
        accountSettingsController.saveGeneral(request, response, mock(Model.class));

        // Then
        verify(liferayAccountService).updateUser(user);
        verify(ldapAccountService).setAttributes(anyString(), anyMap());
        verify(response).setRenderParameter(eq("successMessage"), anyString());
    }

    @Test
    public void testSaveEmail() throws Exception {

        // Given
        ActionRequest request = mock(ActionRequest.class);
        when(request.getParameter("newEmail")).thenReturn("test@email.com");
        when(request.getParameter("confirmEmail")).thenReturn("test@email.com");

        String liferayUserId = "10000";
        when(request.getRemoteUser()).thenReturn(liferayUserId);
        User user = mock(User.class);
        when(liferayAccountService.lookupUser(Long.parseLong(liferayUserId))).thenReturn(user);

        // When
        ActionResponse response = mock(ActionResponse.class);
        accountSettingsController.saveEmail(request, response, mock(Model.class));
        
        // Then
        verify(response).setRenderParameter(eq("successMessage"), anyString());
    }

    @Test
    public void testSavePassword() throws Exception {

        // Given
        ActionRequest request = mock(ActionRequest.class);
        when(request.getParameter("newPassword")).thenReturn("123qwe");
        when(request.getParameter("confirmPassword")).thenReturn("123qwe");
        ActionResponse response = mock(ActionResponse.class);

        // When
        accountSettingsController.savePassword(request, response, mock(Model.class));

        // Then
        verify(response).setRenderParameter(eq("successMessage"), anyString());
    }

    @Test
    public void testHandleException() throws Exception {

        // When
        ModelAndView modelAndView = accountSettingsController.handleException(new Exception("TestException"));

        // Then
        ModelAndView expectedModelAndView = new ModelAndView("errorPage");
        expectedModelAndView.addObject("errorMessage", "Tekniskt fel.");
        assertEquals(expectedModelAndView.getModel(), modelAndView.getModel());
    }
}
