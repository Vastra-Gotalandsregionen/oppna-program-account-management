package se.vgregion.accountmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.vgregion.accountmanagement.LdapException;
import se.vgregion.accountmanagement.PasswordChangeException;
import se.vgregion.ldapservice.LdapUser;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;
import se.vgregion.ldapservice.SimpleLdapUser;

import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service
public class LdapAccountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapAccountService.class);

    @Value("${BASE}")
    private String base;

    @Autowired
    private SimpleLdapServiceImpl simpleLdapService;

    /**
     * Constructor.
     *
     * @param simpleLdapService simpleLdapService
     */
    public LdapAccountService(SimpleLdapServiceImpl simpleLdapService) {
        this.simpleLdapService = simpleLdapService;
    }

    /**
     * Constructor.
     */
    public LdapAccountService() {

    }
    
    public void setEmailInLdap(String uid, String email) throws LdapException {
        LdapUser ldapUser = simpleLdapService.getLdapUserByUid(base, uid);

        if (ldapUser == null) {
            throw new LdapException("Din användare kunde inte hittas i katalogservern.");
        }

        simpleLdapService.getLdapTemplate().getLdapOperations().modifyAttributes(
                ldapUser.getDn(), new ModificationItem[]{new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute("mail", email))});
    }

    /**
     * Updates the LDAP password for a user with a given uid in LDAP.
     *
     * @param uid      the user's uid in LDAP
     * @param password the new password
     * @throws se.vgregion.accountmanagement.PasswordChangeException
     *          PasswordChangeException
     */
    public void setPasswordInLdap(String uid, String password) throws PasswordChangeException {
        SimpleLdapUser ldapUser = (SimpleLdapUser) simpleLdapService.getLdapUserByUid(base, uid);

        if (ldapUser == null) {
            throw new PasswordChangeException("Din användare kunde inte hittas i katalogservern.");
        }

        String encPassword = encryptWithSha(password);

        simpleLdapService.getLdapTemplate().getLdapOperations().modifyAttributes(
                ldapUser.getDn(), new ModificationItem[]{new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute("userPassword", encPassword))});
    }

    String encryptWithSha(String password) {
        String encPassword = null;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA");
            byte[] digest = sha.digest(password.getBytes("UTF-8"));
            encPassword = "{SHA}" + DatatypeConverter.printBase64Binary(digest);
        } catch (UnsupportedEncodingException e) {
            //won't happen
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            //won't happen
            e.printStackTrace();
        }
        return encPassword;
    }

    /**
     * Verifies that the LDAP user with the given uid has the given password.
     *
     * @param uid           the LDAP user's uid
     * @param plainPassword the password in plain text which is to be validated against the password in LDAP
     * @throws se.vgregion.accountmanagement.PasswordChangeException
     *          PasswordChangeException
     */
    public void verifyPasswordWasModifiedInLdap(String uid, String plainPassword) throws PasswordChangeException {
        String encPassword = encryptWithSha(plainPassword);
        SimpleLdapUser ldapUser;
        ldapUser = (SimpleLdapUser) simpleLdapService.getLdapUserByUid(base, uid);
        byte[] userPassword;
        try {
            userPassword = (byte[]) ldapUser.getAttributes(new String[]{"userPassword"}).get("userPassword").get();
            String passwordToVerify = new String(userPassword, "UTF-8");
            if (!encPassword.equals(passwordToVerify)) {
                throw new PasswordChangeException("Lyckades inte byta lösenord i KIV.");
            }
        } catch (NamingException e) {
            throw new PasswordChangeException(e);
        } catch (UnsupportedEncodingException e) {
            //won't happen
            e.printStackTrace();
        }
    }

    public void setAttributes(String userId, Map<String, String> attributes) throws LdapException {
        LdapUser ldapUser = getUser(userId);

        if (ldapUser == null) {
            throw new LdapException("Användare med uid=" + userId + " hittades inte i KIV.");
        }

        ModificationItem[] modificationItems = new ModificationItem[attributes.size()];
        int i = 0;
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            modificationItems[i] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute(attribute.getKey(), attribute.getValue()));
            i++;
        }

        simpleLdapService.getLdapTemplate().getLdapOperations().modifyAttributes(
                ldapUser.getDn(), modificationItems);
    }

    public LdapUser getUser(String userId) {
        return simpleLdapService.getLdapUserByUid("ou=externa,ou=anv,o=VGR", userId);
    }
}
