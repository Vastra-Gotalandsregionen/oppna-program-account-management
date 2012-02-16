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
 * Service for managing accounts in Liferay's database. Contains methods for reading and writing account information.
 *
 * @author Patrik Bergström
 */
@Service
public class LiferayAccountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiferayAccountService.class);

    @Autowired
    private UserLocalService userLocalService;

    /**
     * Finds a {@link User} by userId (a <code>long</code>).
     *
     * @param userId the user's userId
     * @return the {@link User}
     */
    public User lookupUser(Long userId) {

        User user = null;
        try {
            user = userLocalService.getUser(userId);
            if (user == null) {
                String msg = String.format("Användaren med id [%s] finns inte i Liferays användardatabas",
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

    /**
     * Updates a Liferay user by providing a {@link User} object which is to be stored.
     *
     * @param user the {@link User} object to be stored
     * @throws LiferayAccountException LiferayAccountException
     */
    public void updateUser(User user) throws LiferayAccountException {
        try {
            userLocalService.updateUser(user);
        } catch (SystemException e) {
            throw new LiferayAccountException("Tekniskt fel. ", e);
        }
    }
}
