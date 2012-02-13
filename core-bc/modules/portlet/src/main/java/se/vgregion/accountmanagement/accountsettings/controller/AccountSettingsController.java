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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import se.vgregion.accountmanagement.service.LdapAccountService;
import se.vgregion.ldapservice.LdapUser;

import javax.portlet.ActionRequest;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import java.util.Map;

/**
 * Controller class backing up the account settings portlet.
 *
 * @author Patrik Bergström
 */

@Controller
@RequestMapping("VIEW")
public class AccountSettingsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountSettingsController.class);

    @Autowired
    private LdapAccountService ldapAccountService;

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
    public String showAccountSettingsForm(RenderRequest request, Model model) {

        String loggedInUser = lookupP3PInfo(request, PortletRequest.P3PUserInfos.USER_LOGIN_ID);

        System.out.println(loggedInUser);

        LdapUser user = ldapAccountService.getUser(loggedInUser);

        model.addAttribute("firstName", user.getAttributeValue("givenName"));
        model.addAttribute("lastName", user.getAttributeValue("sn"));
        model.addAttribute("email", user.getAttributeValue("mail"));
        model.addAttribute("organization", user.getAttributeValue("externalStructurepersonDN"));
        model.addAttribute("telephone", user.getAttributeValue("telephoneNumber"));
        model.addAttribute("mobile", user.getAttributeValue("mobile"));
//        model.addAttribute("lastName", user.getAttributeValue("sn"));
//        model.addAttribute("lastName", user.getAttributeValue("sn"));
//        model.addAttribute("lastName", user.getAttributeValue("sn"));


        return "accountSettingsForm";
    }

    @ActionMapping
    public void save(ActionRequest request) {
        System.out.println("asdf");
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
