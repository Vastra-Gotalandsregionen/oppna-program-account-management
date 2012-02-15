package se.vgregion.accountmanagement.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.service.UserLocalService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.vgregion.accountmanagement.LiferayAccountException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Patrik Bergström
 */
@RunWith(MockitoJUnitRunner.class)
public class LiferayAccountServiceTest {

    @Mock
    private UserLocalService userLocalService;

    @InjectMocks
    private LiferayAccountService liferayAccountService = new LiferayAccountService();
    
    @Test
    public void testLookupUserSuccess() throws Exception {

        long userId = 10000L;

        // Given
        User user = mock(User.class);
        when(userLocalService.getUser(userId)).thenReturn(user);

        // When
        User user1 = liferayAccountService.lookupUser(userId);

        // Then
        assertEquals(user, user1);
    }

    @Test
    public void testLookupUserFailure() throws Exception {

        long userId = 10000L;

        // Given
        User user = mock(User.class);
        when(userLocalService.getUser(userId)).thenThrow(new PortalException("testException"));

        // When
        User user1 = liferayAccountService.lookupUser(userId);

        // Then
        assertNull(user1);
    }

    @Test(expected = LiferayAccountException.class)
    public void testUpdateUser() throws Exception {

        //Given
        when(userLocalService.updateUser(any(User.class))).thenThrow(new SystemException());

        // When
        User user = mock(User.class);
        liferayAccountService.updateUser(user);
    }
}
