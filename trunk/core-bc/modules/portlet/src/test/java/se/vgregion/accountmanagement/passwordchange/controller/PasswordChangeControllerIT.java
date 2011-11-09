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
import se.vgregion.accountmanagement.passwordchange.PasswordChangeException;
import se.vgregion.http.HttpRequest;
import se.vgregion.ldapservice.SimpleLdapServiceImpl;

import javax.xml.bind.JAXBException;
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
    @Value("${basic_authentication.username}")
    private String basicUserName;
    @Value("${basic_authentication.password}")
    private String basicUserPassword;

    @Autowired
    private SimpleLdapServiceImpl simpleLdapService;

    @Test
    @Ignore //Run this when needed. It changes (if successful) the domino and ldap password for a given user.
    public void setDominoPassword() throws MessageBusException, PasswordChangeException, JAXBException {
        Message message = new Message();
        String userVgrId = "xxtst1";
        String newUserPassword = "password2";
        String queryString = String.format("Openagent&username=%s&password=%s", userVgrId, newUserPassword);
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setQueryByString(queryString);
        httpRequest.addBasicAuthentication(basicUserName, basicUserPassword);
        //see se.vgregion.messagebus.EndpointMessageListener.createExchange() to see how the payload object is handled
        message.setPayload(httpRequest);

        DefaultSynchronousMessageSender sender = createMessageSender();

        Object result = sender.send(messagebusDestination, message, 15000);

        System.out.println(result);

//        result = "<html><head></head><body text=\"#0000\">Content</body></html>";
//        JAXBContext jc = JAXBContext.newInstance(Html.class);
//        Html html = (Html) jc.createUnmarshaller().unmarshal(new StringReader((String) result));
//        System.out.println(html.getBody().getText());
//        System.out.println(html.getBody().getValue());
//        System.out.println(html.getHead());

        //verify it has been set in ldap
        PasswordChangeController passwordChangeController = new PasswordChangeController(simpleLdapService);
        passwordChangeController.verifyPasswordWasModified(userVgrId, passwordChangeController.encryptWithMd5(
                newUserPassword));
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
