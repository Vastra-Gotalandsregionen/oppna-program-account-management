/**
 * Copyright 2010 Västra Götalandsregionen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public
 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *   Boston, MA 02111-1307  USA
 *
 */

package se.vgregion.accountmanagement.accountsettings.controller;

import com.liferay.portal.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import se.vgregion.accountmanagement.LdapException;
import se.vgregion.accountmanagement.LiferayAccountException;
import se.vgregion.accountmanagement.PasswordChangeException;
import se.vgregion.accountmanagement.ValidationException;
import se.vgregion.accountmanagement.service.LdapAccountService;
import se.vgregion.accountmanagement.service.LiferayAccountService;
import se.vgregion.accountmanagement.util.ValidationUtils;
import se.vgregion.ldapservice.LdapUser;

import javax.portlet.*;
import java.util.Map;

/**
 * Controller class backing up the account settings portlet.
 *
 * @author Patrik Bergström
 */

@Controller
@RequestMapping("VIEW")
@SessionAttributes("selectedTab")
public class AccountSettingsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountSettingsController.class);

    private static final byte GENERAL_SETTINGS_TAB_INDEX = 1;
    private static final byte EMAIL_SETTINGS_TAB_INDEX = 2;
    private static final byte PASSWORD_SETTINGS_TAB_INDEX = 3;

    @Autowired
    private LdapAccountService ldapAccountService;

    @Autowired
    private LiferayAccountService liferayAccountService;

    /**
     * Constructor.
     */
    public AccountSettingsController() {

    }

    public void setLdapAccountService(LdapAccountService ldapAccountService) {
        this.ldapAccountService = ldapAccountService;
    }

    /**
     * Handler method called by Spring.
     *
     * @param request request
     * @param model   model
     * @return the accountSettingsForm view
     */
    @RenderMapping
    public String showAccountSettingsForm(RenderRequest request, RenderResponse response, Model model) {

        String loggedInUser = lookupP3PInfo(request, PortletRequest.P3PUserInfos.USER_LOGIN_ID);

        String[] errorMessages = request.getParameterMap().get("errorMessage");
        if (errorMessages != null && errorMessages.length > 0) {
            // There should only be one
            model.addAttribute("errorMessage", errorMessages[0]);
        }

        String[] successMessages = request.getParameterMap().get("successMessage");
        if (successMessages != null && successMessages.length > 0) {
            model.addAttribute("successMessage", successMessages[0]);
        }

        LdapUser user = ldapAccountService.getUser(loggedInUser);

        model.addAttribute("firstName", user.getAttributeValue("givenName"));
        model.addAttribute("lastName", user.getAttributeValue("sn"));
        model.addAttribute("email", user.getAttributeValue("mail"));
        model.addAttribute("telephone", user.getAttributeValue("telephoneNumber"));
        model.addAttribute("mobile", user.getAttributeValue("mobile"));
        model.addAttribute("organization", user.getAttributeValue("externalStructurepersonDN"));

        return "accountSettingsForm";
    }

    @ActionMapping(params = "action=saveGeneral")
    public void saveGeneral(ActionRequest request, Model model) throws LiferayAccountException {
        model.addAttribute("selectedTab", GENERAL_SETTINGS_TAB_INDEX);
        
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String telephone = request.getParameter("telephone");
        String mobile = request.getParameter("mobile");
        String organization = request.getParameter("organization");

        User user = liferayAccountService.lookupUser(Long.valueOf(request.getRemoteUser()));

        // Set available fields
        user.setFirstName(firstName);
        user.setLastName(lastName);

        // Save
        liferayAccountService.updateUser(user);

        System.out.println("saveGeneralActionUrl");
    }

    @ActionMapping(params = "action=saveEmail")
    public void saveEmail(ActionRequest request, ActionResponse response, Model model) {
        model.addAttribute("selectedTab", EMAIL_SETTINGS_TAB_INDEX);
        
        String newEmail = request.getParameter("newEmail");
        String confirmEmail = request.getParameter("confirmEmail");

        try {
            ValidationUtils.validateEmail(newEmail, confirmEmail);

            User user = liferayAccountService.lookupUser(Long.valueOf(request.getRemoteUser()));

            user.setEmailAddress(newEmail);
            
            // Update Liferay
            liferayAccountService.updateUser(user);

            // Update LDAP
            String loggedInUser = lookupP3PInfo(request, PortletRequest.P3PUserInfos.USER_LOGIN_ID);
            ldapAccountService.setEmailInLdap(loggedInUser, newEmail);
            response.setRenderParameter("successMessage", "E-post uppdaterad.");
            
        } catch (ValidationException ex) {
            response.setRenderParameter("errorMessage", ex.getMessage());
        } catch (LiferayAccountException ex) {
            LOGGER.error(ex.getMessage(), ex);
            response.setRenderParameter("errorMessage", ex.getMessage());
        } catch (LdapException ex) {
            LOGGER.error(ex.getMessage(), ex);
            response.setRenderParameter("errorMessage", ex.getMessage());
        }

    }

    @ActionMapping(params = "action=savePassword")
    public void savePassword(ActionRequest request, ActionResponse response, Model model) throws ValidationException {
        model.addAttribute("selectedTab", PASSWORD_SETTINGS_TAB_INDEX);
        
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        ValidationUtils.validatePassword(newPassword, confirmPassword);

        // Update LDAP only. Liferay only relies on the password in LDAP.
        String loggedInUser = lookupP3PInfo(request, PortletRequest.P3PUserInfos.USER_LOGIN_ID);
        try {
            ldapAccountService.setPasswordInLdap(loggedInUser, newPassword);
            response.setRenderParameter("successMessage", "Lösenordet uppdaterat.");
        } catch (PasswordChangeException ex) {
            LOGGER.error(ex.getMessage(), ex);
            response.setRenderParameter("errorMessage", ex.getMessage());
        }

    }

    /**
     * Handler method for when an <code>Exception</code> is thrown.
     *
     * @param ex Exception
     * @return A <code>ModelAndView</code>
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception ex) {
        LOGGER.error(ex.getMessage(), ex);
        ModelAndView modelAndView = new ModelAndView("errorPage");
        modelAndView.addObject("errorMessage", ex.getMessage());
        return modelAndView;
    }

    private String lookupP3PInfo(PortletRequest req, PortletRequest.P3PUserInfos p3pInfo) {
        Map<String, String> userInfo = (Map<String, String>) req.getAttribute(PortletRequest.USER_INFO);
        String info;
        if (userInfo != null) {
            info = userInfo.get(p3pInfo.toString());
        } else {
            return null;
        }
        return info;
    }
    
}
