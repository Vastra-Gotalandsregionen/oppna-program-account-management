package se.vgregion.accountmanagement.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.service.UserLocalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.vgregion.accountmanagement.LiferayAccountException;

/**
 * @author Patrik Bergström
 */
@Service
public class LiferayAccountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiferayAccountService.class);

    @Autowired
    private UserLocalService userLocalService;

    public User lookupUser(Long userId) {

        User user = null;
        try {
            user = userLocalService.getUser(userId);
            if (user == null) {
                String msg = String.format("Användaren med id [%s] finns inte i Liferays användar databas",
                        userId);
                LOGGER.warn(msg, new IllegalArgumentException("Användaren hittades inte"));
            }
        } catch (PortalException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (SystemException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return user;
    }

    public void updateUser(User user) throws LiferayAccountException {
        try {
            userLocalService.updateUser(user);
        } catch (SystemException e) {
            throw new LiferayAccountException("Tekniskt fel. ", e);
        }
    }
}
