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

package se.vgregion.accountmanagement.passwordchange.controller;

import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import se.vgregion.accountmanagement.PasswordChangeException;
import se.vgregion.accountmanagement.service.LdapAccountService;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;

/**
 * Controller class backing up the password change portlet.
 *
 * @author Patrik Bergström
 */

@Controller
@RequestMapping("VIEW")
public class PasswordChangeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordChangeController.class);

    @Autowired
    private LdapAccountService ldapAccountService;

    /**
     * Constructor.
     */
    public PasswordChangeController() {

    }

    /**
     * Handler method called by Spring.
     *
     * @param request request
     * @param model   model
     * @return the passwordChangeForm view
     * @throws PasswordChangeException PasswordChangeException
     */
    @RenderMapping
    public String showPasswordChangeForm(RenderRequest request, Model model) throws PasswordChangeException {

        //lookup user's vgr id
        String screenName = lookupScreenName(request);
        if (screenName != null) {
            model.addAttribute("vgrId", screenName);
        } else {
            model.addAttribute("errorMessage", "Kunde inte hitta ditt vgr-id.");
        }

        return "passwordChangeForm";
    }


    private String lookupScreenName(PortletRequest request) {
        String screenName;
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        if (themeDisplay.getUser() != null) {
            screenName = themeDisplay.getUser().getScreenName();
            if (screenName != null) {
                return screenName;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Handler method called by Spring.
     *
     * @return the success view
     */
    @RenderMapping(params = "success")
    public String showSuccessPage() {
        return "success";
    }

    /**
     * Handler method called by Spring. It changes the user's password. If the user is a domino user a web service will
     * be called which will update the password in both the LDAP catalog and Domino. If the user is not a domino user a
     * direct call to the LDAP will be made to update the password.
     *
     * @param request  request
     * @param response response
     * @param model    model
     * @throws PasswordChangeException PasswordChangeException
     */
    @ActionMapping(params = "action=changePassword")
    public void changePassword(ActionRequest request, ActionResponse response, Model model)
            throws PasswordChangeException {
        String password = request.getParameter("password");
        String passwordConfirm = request.getParameter("passwordConfirm");

        try {
            //lookup user's vgr id
            String screenName = lookupScreenName(request);

            //validate
            validatePassword(password, passwordConfirm);

            //no domino -> continue with setting password in LDAP only, directly
            ldapAccountService.setPasswordInLdap(screenName, password);
            ldapAccountService.verifyPasswordWasModifiedInLdap(screenName, password);

            response.setRenderParameter("success", "success");
        } catch (PasswordChangeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            LOGGER.warn(ex.getMessage(), ex);
        }
    }

    protected void validatePassword(String password, String passwordConfirm) throws PasswordChangeException {
        if (password != null) {
            if (!password.equals(passwordConfirm)) {
                throw new PasswordChangeException("Lösenorden matchar inte.");
            }
            //validate strength
            final int i = 6;
            if (password.length() < i) {
                throw new PasswordChangeException("Lösenordet måste vara minst 6 tecken.");
            }
            if (!password.matches("[a-zA-Z0-9]*")) {
                throw new PasswordChangeException("Lösenordet får bara innehålla bokstäver och siffror");
            }
            if (!(password.matches(".*[a-zA-Z]+.*") && password.matches(".*[0-9]+.*"))) {
                throw new PasswordChangeException("Lösenordet måste innehålla både bokstäver och siffror");
            }
        } else {
            throw new PasswordChangeException("Fyll i lösenord.");
        }
    }

    public void setLdapAccountService(LdapAccountService ldapAccountService) {
        this.ldapAccountService = ldapAccountService;
    }
}