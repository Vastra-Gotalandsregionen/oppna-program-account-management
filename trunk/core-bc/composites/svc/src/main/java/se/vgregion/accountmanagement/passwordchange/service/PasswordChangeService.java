package se.vgregion.accountmanagement.passwordchange.service;

import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageBusException;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.vgregion.accountmanagement.domain.DominoResponse;
import se.vgregion.accountmanagement.passwordchange.PasswordChangeException;
import se.vgregion.http.HttpRequest;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;
import se.vgregion.ldapservice.SimpleLdapUser;
import se.vgregion.portal.cs.domain.UserSiteCredential;
import se.vgregion.portal.cs.service.CredentialService;
import se.vgregion.util.JaxbUtil;

import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class PasswordChangeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordChangeService.class);

    @Autowired
    private Ehcache ehcache;
    @Autowired
    private CredentialService credentialService;
    @Autowired
    private SimpleLdapServiceImpl simpleLdapService;

    @Value("${changepassword.messagebus.destination}")
    private String changePasswordMessagebusDestination;
    @Value("${verifypassword.messagebus.destination}")
    private String verifyPasswordMessagebusDestination;
    @Value("${admin_authentication.username}")
    private String adminUsername;
    @Value("${admin_authentication.password}")
    private String adminPassword;
    @Value("${BASE}")
    private String base;

    private final int defaultLimit = 15 * 60; // fifteen minutes
    private int limit = defaultLimit;
    private final int defaultDelay = 10000; // ten seconds
    private int delay = defaultDelay;

    /**
     * Constructor.
     *
     * @param simpleLdapService simpleLdapService
     */
    public PasswordChangeService(SimpleLdapServiceImpl simpleLdapService) {
        this.simpleLdapService = simpleLdapService;
    }

    /**
     * Constructor.
     */
    public PasswordChangeService() {

    }

    public void setLimit(int seconds) {
        this.limit = seconds;
    }

    public void setDelay(int millis) {
        this.delay = millis;
    }

    /**
     * Looks up the time that has passed since a user's password was updated (only applicable to Domino users).
     *
     * @param screenName the user's screenName
     * @return time in seconds or <code>null</code> if there is not record in the cache
     */
    public Long lookupSecondsElapsed(String screenName) {
        Element element = ehcache.get(screenName);
        Long secondsElapsed = null;
        if (element != null) {
            final int i = 1000;
            secondsElapsed = (System.currentTimeMillis() - element.getLatestOfCreationAndUpdateTime()) / i;
        }
        return secondsElapsed;
    }

    void notifyPasswordChange(String screenName, String password) {
        ehcache.put(new Element(screenName, password));
    }

    /**
     * This method calls a service which in turn updates the password in Domino and LDAP. In addition a monitoring
     * activity is started to check when the password is updated in Domino (because it can take a while) and when the
     * password is updated in Domino the user's CSIframe password for Domino-related services is updated. Thus, the user
     * will be able to use the services without interruption.
     *
     * @param screenName the user's screenName
     * @param password   the user's new password
     * @throws PasswordChangeException PasswordChangeException
     */
    public void updateDominoLdapAndInotes(String screenName, String password) throws PasswordChangeException {
        try {
            setDominoAndLdapPassword(password, screenName);
            verifyPasswordWasModifiedInLdap(screenName, password);
        } catch (PasswordChangeException ex) {
            throw new PasswordChangeException("Ditt lösenord har uppdaterats i Domino men inte i KIV.", ex);
        } catch (MessageBusException ex) {
            throw new PasswordChangeException("Tekniskt fel. Försök igen senare.", ex);
        }
        notifyPasswordChange(screenName, password);

        monitorPasswordUpdateAndUpdateInotes(screenName, password);
    }

    protected void monitorPasswordUpdateAndUpdateInotes(final String screenName, final String password) {
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (verifyUserPassword(screenName, password)) {
                    //The password is updated in domino. Let's update the iNotes password for CSIframe
                    LOGGER.info("Password has been updated in Domino, so update CSIframe iNotes password.");
                    updateCredentialStoreInotes(screenName, password);
                    timer.cancel();
                    ehcache.remove(screenName);
                } else {
                    LOGGER.debug("Password is not yet updated in Domino. Will check again.");
                    // If more than fifteen minutes have passed we give up
                    Element element = ehcache.get(screenName);
                    final float thousand = 1000f;
                    final float sixty = 60f;
                    if ((System.currentTimeMillis() - element.getLatestOfCreationAndUpdateTime()) / thousand > limit) {
                        LOGGER.info("Domino password has not been updated for " + limit + " seconds. Giving up.");
                        ehcache.remove(screenName);
                        timer.cancel();
                    }
                }
            }
        }, delay, delay);
    }

    boolean verifyUserPassword(String screenName, String password) {
        Message message = new Message();
        String queryString = String.format("Openagent&username=%s&password=%s", screenName, password);
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setQueryByString(queryString);
        //see se.vgregion.messagebus.EndpointMessageListener.createExchange() to see how the payload object is handled
        message.setPayload(httpRequest);

        try {
            final int timeout = 15000;
            Object reply = MessageBusUtil.sendSynchronousMessage(verifyPasswordMessagebusDestination, message, timeout);

            if (reply == null) {
                throw new MessageBusException("No reply was given. Is destination ["
                        + verifyPasswordMessagebusDestination + "] really configured?");
            } else if (reply instanceof String) {
                try {
                    JaxbUtil jaxbUtil = new JaxbUtil(DominoResponse.class);
                    DominoResponse response = jaxbUtil.unmarshal((String) reply);
                    return response.getStatusmessage().equals("1"); //1 == success
                } catch (JAXBException e) {
                    LOGGER.error("Failed to parse reply: " + System.getProperty("line.separator") + reply, e);
                    return false;
                }
            } else if (reply instanceof Throwable) {
                throw new MessageBusException((Throwable) reply);
            }
        } catch (MessageBusException e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

    protected void setDominoAndLdapPassword(String password, String screenName) throws PasswordChangeException,
            MessageBusException {
        Message message = new Message();
        String queryString = String.format("Openagent&username=%s&password=%s&adminUserName=%s"
                + "&adminPassword=%s", screenName, password, adminUsername, adminPassword);
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setQueryByString(queryString);

        //see se.vgregion.messagebus.EndpointMessageListener.createExchange() to see how the payload object
        //is handled
        message.setPayload(httpRequest);

        //make call to change password
        final int timeout = 15000;
        Object reply = MessageBusUtil.sendSynchronousMessage(changePasswordMessagebusDestination, message, timeout);

        if (reply == null) {
            throw new MessageBusException("No reply was given. Is destination [" + changePasswordMessagebusDestination
                    + "] really configured?");
        } else if (reply instanceof String) {
            try {
                JaxbUtil jaxbUtil = new JaxbUtil(DominoResponse.class);
                DominoResponse response = jaxbUtil.unmarshal((String) reply);
                if (response.getStatuscode() != 1) { //1 == success
                    throw new PasswordChangeException("Misslyckades att sätta lösenord i Domino. "
                            + response.getStatusmessage());
                }
            } catch (JAXBException e) {
                throw new PasswordChangeException(e);
            }
        } else if (reply instanceof Throwable) {
            throw new MessageBusException((Throwable) reply);
        }
    }

    void updateCredentialStoreInotes(String screenName, String password) {
        UserSiteCredential credential = credentialService.getUserSiteCredential(screenName, "iNotes");
        if (credential == null) {
            credential = new UserSiteCredential(screenName, "iNotes");
        }
        credential.setSitePassword(password);
        credential.setSiteUser(screenName);
        credentialService.save(credential);
    }

    /**
     * Updates the LDAP password for a user with a given uid in LDAP.
     *
     * @param uid the user's uid in LDAP
     * @param password the new password
     * @throws PasswordChangeException PasswordChangeException
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
     * @param uid the LDAP user's uid
     * @param plainPassword the password in plain text which is to be validated against the password in LDAP
     * @throws PasswordChangeException PasswordChangeException
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

    /**
     * Whether the password has been requested for update but has not yet been applied in Domino since it can take a
     * while. The update request is stored in the cache for a while and removed if the update has been applied in
     * Domino or if sufficient time has elapsed.
     *
     * @param screenName the user's screenName
     * @return <code>true</code> if the update is stored in the cache or <code>false</code> otherwise
     */
    public boolean isPasswordUpdateInProgress(String screenName) {
        return ehcache.get(screenName) != null;
    }
}
