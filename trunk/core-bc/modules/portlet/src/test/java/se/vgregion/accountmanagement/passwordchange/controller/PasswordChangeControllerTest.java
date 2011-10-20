package se.vgregion.accountmanagement.passwordchange.controller;

import junit.framework.TestCase;
import org.mockito.ArgumentCaptor;
import org.springframework.ui.Model;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import static org.mockito.Mockito.*;

/**
 * @author Patrik Bergström
 */
public class PasswordChangeControllerTest extends TestCase {

    private PasswordChangeController controller = new PasswordChangeController();

    //A method counts as a method in the test coverage statistics ;)
    public void testShowPasswordChangeForm() throws Exception {
        String view = controller.showPasswordChangeForm();
        assertNotNull(view);
    }

    public void testShowPasswordChangeFormWithError() throws Exception {
        String errorMessage = "Ett felmeddelande.";
        Model model = mock(Model.class);
        String view = controller.showPasswordChangeFormWithError(errorMessage, model);

        verify(model).addAttribute(eq("errorMessage"), eq(errorMessage));

    }

    public void testShowSuccessPage() throws Exception {
        String view = controller.showSuccessPage();
        assertNotNull(view);
    }

    public void testChangePassword() throws Exception {
        ActionResponse response = mock(ActionResponse.class);
        controller.changePassword(mock(ActionRequest.class), response);

        verify(response).setRenderParameter("success", "success");
    }
}
