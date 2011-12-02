package se.vgregion.accountmanagement.passwordchange.controller;

import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageBus;
import com.liferay.portal.kernel.messaging.MessageBusException;
import com.liferay.portal.kernel.messaging.sender.DefaultSynchronousMessageSender;
import com.liferay.portal.kernel.uuid.PortalUUID;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.vgregion.accountmanagement.domain.DominoResponse;
import se.vgregion.accountmanagement.passwordchange.PasswordChangeException;
import se.vgregion.http.HttpRequest;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;
import se.vgregion.ldapservice.SimpleLdapUser;
import se.vgregion.util.JaxbUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringReader;
import java.util.Random;

/**
 * @author Patrik Bergström
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "/messagebus-changepassword-routes-test.xml",
        "/ldapService-test.xml"}) //to get the ldap service
public class PasswordChangeControllerIT extends TestCase {

    @Autowired
    private MessageBus messageBus;

    //looks up the properties via Spring's PropertyPlaceholder
    @Value("${changepassword.messagebus.destination}")
    private String messagebusDestination;
    @Value("${admin_authentication.username}")
    private String adminUserName;
    @Value("${admin_authentication.password}")
    private String adminUserPassword;

    @Autowired
    private SimpleLdapServiceImpl simpleLdapService;

    @Test
    @Ignore //Run this when needed. It changes (if successful) the domino and ldap password for a given user.
    public void setDominoPassword() throws MessageBusException, PasswordChangeException, JAXBException {
        Message message = new Message();
        String userVgrId = "xxtst1";
        String newUserPassword = "password3";
        String queryString = String.format("Openagent&username=%s&password=%s&adminUserName=%s&adminPassword=%s",
                userVgrId, newUserPassword, adminUserName, adminUserPassword);
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setQueryByString(queryString);
        //see se.vgregion.messagebus.EndpointMessageListener.createExchange() to see how the payload object is handled
        message.setPayload(httpRequest);

        DefaultSynchronousMessageSender sender = createMessageSender();

        Object result = sender.send(messagebusDestination, message, 15000);

        System.out.println(result);

        if (result instanceof String) {
            JaxbUtil jaxbUtil = new JaxbUtil(DominoResponse.class);
            DominoResponse response = jaxbUtil.unmarshal((String) result);

            if (response.getStatuscode() != 1) {
                throw new PasswordChangeException("Failed to change password in Domino.");
            }
        } else {
            fail();
        }

        //verify it has been set in ldap
        PasswordChangeController passwordChangeController = new PasswordChangeController(simpleLdapService);
        passwordChangeController.verifyPasswordWasModified(userVgrId, passwordChangeController.encryptWithSha(
                newUserPassword));
    }

    @Test
    public void testLdapSearch() {
        SimpleLdapUser ldapUser = (SimpleLdapUser) simpleLdapService.getLdapUserByUid("", "ex_teste");

        assertNotNull(ldapUser);
    }

    private DefaultSynchronousMessageSender createMessageSender() {
        //just to make a working sender without having a real Liferay server running
        DefaultSynchronousMessageSender sender = new DefaultSynchronousMessageSender();
        sender.setPortalUUID(new PortalUUID() {
            @Override
            public String generate() {
                Random random = new Random();
                return random.nextInt() + "";
            }
        });
        sender.setMessageBus(messageBus);
        return sender;
    }

}
