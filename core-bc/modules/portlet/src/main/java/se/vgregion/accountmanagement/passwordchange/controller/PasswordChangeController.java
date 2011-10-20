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

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import se.vgregion.accountmanagement.passwordchange.PasswordChangeException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

/**
 * @author Patrik Bergström
 */

@Controller
@RequestMapping("VIEW")
public class PasswordChangeController {

    @RenderMapping
    public String showPasswordChangeForm() {
        return "passwordChangeForm";
    }

    @RenderMapping(params = "failure")
    public String showPasswordChangeFormWithError(@RequestParam(value = "failure") String errorMessage, Model model) {
        model.addAttribute("errorMessage", errorMessage);
        return "passwordChangeForm";
    }

    @ActionMapping(params = "action=changePassword")
    public void changePassword(ActionRequest request, ActionResponse response) throws PasswordChangeException {
        String password = request.getParameter("password");
        String passwordConfirm = request.getParameter("passwordConfirm");

        try {
            //validate
            if (password != null) {
                if (!password.equals(passwordConfirm)) {
                    throw new PasswordChangeException("Lösenorden matchar inte.");
                }
                //TODO what password policy do we have?
                //validate strength
                final int i = 6;
                if (password.length() < i) {
                    throw new PasswordChangeException("Lösenordet måste vara minst 6 tecken.");
                }
            } else {
                throw new PasswordChangeException("Fyll i lösenord");
            }

            //make call to change password

        } catch (PasswordChangeException ex) {
            response.setRenderParameter("failure", ex.getMessage());
        }
    }
}
