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

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageBusException;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Role;
import com.liferay.portal.theme.ThemeDisplay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import se.vgregion.accountmanagement.passwordchange.PasswordChangeException;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;
import se.vgregion.ldapservice.SimpleLdapUser;

import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Patrik Bergström
 */

@Controller
@RequestMapping("VIEW")
public class PasswordChangeController {

    @Autowired
    private SimpleLdapServiceImpl simpleLdapService;

    @Value("${changepassword.messagebus.destination}")
    private String messagebusDestination;

    @RenderMapping
    public String showPasswordChangeForm(RenderRequest request, Model model) {

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

    @RenderMapping(params = "failure")
    public String showPasswordChangeFormWithError(@RequestParam(value = "failure") String errorMessage, Model model) {
        model.addAttribute("errorMessage", errorMessage);
        return "passwordChangeForm";
    }

    @RenderMapping(params = "success")
    public String showSuccessPage() {
        return "success";
    }

    @ActionMapping(params = "action=changePassword")
    public void changePassword(ActionRequest request, ActionResponse response) throws PasswordChangeException {
        String password = request.getParameter("password");
        String passwordConfirm = request.getParameter("passwordConfirm");

        try {
            //lookup user's vgr id
            String screenName = lookupScreenName(request);

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
                throw new PasswordChangeException("Fyll i lösenord.");
            }

            boolean isDomino = isDominoUser(request);

            if (isDomino) {
                Message message = new Message();
                Map<String, String> params = new HashMap<String, String>();
                params.put("uid", screenName);
                params.put("password", password);
                message.setPayload(params);

                //make call to change password
                final int timeout = 10000;
                Object reply = MessageBusUtil.sendSynchronousMessage(messagebusDestination, message, timeout);

                if (reply == null) {
                    throw new MessageBusException("No reply was given. Is destination [" + messagebusDestination
                            + "] really configured?");
                } else if (reply instanceof Throwable) {
                    throw new MessageBusException((Throwable) reply);
                }
            } else {
                //no domino -> continue with setting password in LDAP only, directly
                setPasswordInLdap(screenName, password);
            }

            response.setRenderParameter("success", "success");

        } catch (PasswordChangeException ex) {
            response.setRenderParameter("failure", ex.getMessage());
        } catch (MessageBusException e) {
            response.setRenderParameter("failure", "Det gick inte att ändra lösenord. Försök igen senare.");
            e.printStackTrace();
        }
    }

    boolean isDominoUser(ActionRequest request) throws PasswordChangeException {
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        try {
            List<Role> roles = themeDisplay.getUser().getRoles();
            if (roles != null) {
                for (Role role : roles) {
                    String title = role.getTitle();
                    if (title != null && title.toLowerCase().contains("domino")) {
                        return true;
                    }
                }
            }
            //no domino role found
            return false;
        } catch (SystemException e) {
            throw new PasswordChangeException(e);
        }
    }

    protected void setPasswordInLdap(String uid, String password) throws PasswordChangeException {
        SimpleLdapUser ldapUser = (SimpleLdapUser) simpleLdapService.getLdapUserByUid(uid);

        if (ldapUser == null) {
            throw new PasswordChangeException("Din användare kunde inte hittas i katalogservern.");
        }

        //todo temporärt för att inte göra testmissar
        if (!uid.equals("ex_teste")) {
            throw new PasswordChangeException("tillfälligt att bara ex_teste kan ändra sitt lösenord");
        }

        String encPassword = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(password.getBytes("UTF-8"));
            encPassword = "{MD5}" + DatatypeConverter.printBase64Binary(digest);
        } catch (UnsupportedEncodingException e) {
            //won't happen
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            //won't happen
            e.printStackTrace();
        }
        simpleLdapService.getLdapTemplate().getLdapOperations().modifyAttributes(
                ldapUser.getDn(), new ModificationItem[]{new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute("userPassword", encPassword))});

        //verify
        ldapUser = (SimpleLdapUser) simpleLdapService.getLdapUserByUid(uid);
        byte[] userPassword;
        try {
            userPassword = (byte[]) ldapUser.getAttributes(new String[]{"userPassword"}).get("userPassword").get();
            String passwordToVerify = new String(userPassword, "UTF-8");
            if (!encPassword.equals(passwordToVerify)) {
                throw new PasswordChangeException("Lyckades inte byta lösenord.");
            }
        } catch (NamingException e) {
            throw new PasswordChangeException(e);
        } catch (UnsupportedEncodingException e) {
            //won't happen
            e.printStackTrace();
        }

    }
}
