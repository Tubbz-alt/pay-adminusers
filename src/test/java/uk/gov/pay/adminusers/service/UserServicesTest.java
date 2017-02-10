package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.pay.adminusers.model.Link;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.pay.adminusers.resources.UserResource.USERS_RESOURCE;

public class UserServicesTest {

    private UserDao userDao;
    private RoleDao roleDao;
    private ServiceDao serviceDao;
    private PasswordHasher passwordHasher;
    private UserServices userServices;
    private LinksBuilder linksBuilder;

    @Before
    public void before() throws Exception {
        userDao = mock(UserDao.class);
        roleDao = mock(RoleDao.class);
        serviceDao = mock(ServiceDao.class);
        passwordHasher = mock(PasswordHasher.class);
        linksBuilder = new LinksBuilder("http://localhost");
        int testLoginAttemptCap = 3;
        userServices = new UserServices(userDao, roleDao, serviceDao, passwordHasher, linksBuilder, testLoginAttemptCap);
    }

    @Test(expected = WebApplicationException.class)
    public void shouldError_ifRoleNameDoesNotExist() throws Exception {
        User user = aUser();
        String nonExistentRole = "nonExistentRole";
        when(roleDao.findByRoleName(nonExistentRole)).thenReturn(Optional.empty());
        userServices.createUser(user, nonExistentRole);
    }

    @Test
    public void shouldPersistAUser_creatingANewServiceForTheUsersGatewayAccount_whenPersistingTheUserWithNoServiceRelatedToTheGivenGateway() throws Exception {

        User user = aUser();
        Role role = Role.role(2, "admin", "admin role");
        ArgumentCaptor<ServiceEntity> expectedService = ArgumentCaptor.forClass(ServiceEntity.class);

        when(roleDao.findByRoleName(role.getName())).thenReturn(Optional.of(new RoleEntity(role)));
        when(passwordHasher.hash("random-password")).thenReturn("the hashed random-password");
        when(serviceDao.findByGatewayAccountId(user.getGatewayAccountId())).thenReturn(Optional.empty());

        doNothing().when(serviceDao).persist(any(ServiceEntity.class));
        doNothing().when(userDao).persist(any(UserEntity.class));

        User persistedUser = userServices.createUser(user, role.getName());
        Link selfLink = Link.from(Link.Rel.self, "GET", "http://localhost" + USERS_RESOURCE + "/random-name");

        assertThat(persistedUser.getUsername(), is(user.getUsername()));
        assertThat(persistedUser.getPassword(), is(not(user.getPassword())));
        assertThat(persistedUser.getEmail(), is(user.getEmail()));
        assertThat(persistedUser.getGatewayAccountId(), is(user.getGatewayAccountId()));
        assertThat(persistedUser.getTelephoneNumber(), is(user.getTelephoneNumber()));
        assertThat(persistedUser.getOtpKey(), is(user.getOtpKey()));
        assertThat(persistedUser.getRoles().size(), is(1));
        assertThat(persistedUser.getRoles().get(0), is(role));
        assertThat(persistedUser.getLinks().get(0), is(selfLink));

        verify(serviceDao).persist(expectedService.capture());
        assertThat(expectedService.getValue().getGatewayAccount().getGatewayAccountId(), is(user.getGatewayAccountId()));
    }

    @Test
    public void shouldPersist_aUserSuccessfully_andAssociateToTheServiceRelatedToExistingGatewayAccount() throws Exception {

        User user = aUser();
        Role role = Role.role(2, "admin", "admin role");
        ServiceEntity existingServiceRelatedToTheGatewayAccountPassedIn = new ServiceEntity(user.getGatewayAccountId());
        long serviceId = 4;

        existingServiceRelatedToTheGatewayAccountPassedIn.setId(serviceId);
        ArgumentCaptor<UserEntity> expectedUser = ArgumentCaptor.forClass(UserEntity.class);

        when(roleDao.findByRoleName(role.getName())).thenReturn(Optional.of(new RoleEntity(role)));
        when(passwordHasher.hash("random-password")).thenReturn("the hashed random-password");
        when(serviceDao.findByGatewayAccountId(user.getGatewayAccountId())).thenReturn(Optional.of(existingServiceRelatedToTheGatewayAccountPassedIn));
        doNothing().when(userDao).persist(any(UserEntity.class));

        User persistedUser = userServices.createUser(user, role.getName());
        Link selfLink = Link.from(Link.Rel.self, "GET", "http://localhost" + USERS_RESOURCE + "/random-name");

        assertThat(persistedUser.getUsername(), is(user.getUsername()));
        assertThat(persistedUser.getPassword(), is(not(user.getPassword())));
        assertThat(persistedUser.getEmail(), is(user.getEmail()));
        assertThat(persistedUser.getGatewayAccountId(), is(user.getGatewayAccountId()));
        assertThat(persistedUser.getTelephoneNumber(), is(user.getTelephoneNumber()));
        assertThat(persistedUser.getOtpKey(), is(user.getOtpKey()));
        assertThat(persistedUser.getRoles().size(), is(1));
        assertThat(persistedUser.getRoles().get(0), is(role));
        assertThat(persistedUser.getLinks().get(0), is(selfLink));

        verify(serviceDao).findByGatewayAccountId(user.getGatewayAccountId());
        verifyNoMoreInteractions(serviceDao);
        verify(userDao).persist(expectedUser.capture());

        assertThat(expectedUser.getValue().getGatewayAccountId(), is(user.getGatewayAccountId()));
    }

    @Test
    public void shouldFindAUserByUserName() throws Exception {
        User user = aUser();

        UserEntity userEntity = UserEntity.from(user);
        userEntity.setService(new ServiceEntity(user.getGatewayAccountId()));
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByUsername("random-name")).thenReturn(userEntityOptional);

        Optional<User> userOptional = userServices.findUser("random-name");
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getUsername(), is("random-name"));
    }

    @Test
    public void shouldReturnEmpty_WhenFindByUserName_ifNotFound() throws Exception {
        when(userDao.findByUsername("random-name")).thenReturn(Optional.empty());

        Optional<User> userOptional = userServices.findUser("random-name");
        assertFalse(userOptional.isPresent());
    }

    @Test
    public void shouldReturnUserAndResetLoginCount_ifAuthenticationSuccessful() throws Exception {
        User user = aUser();
        user.setLoginCounter(2);

        UserEntity userEntity = UserEntity.from(user);
        userEntity.setService(new ServiceEntity(user.getGatewayAccountId()));
        userEntity.setPassword("hashed-password");
        when(passwordHasher.isEqual("random-password", "hashed-password")).thenReturn(true);
        when(userDao.findByUsername("random-name")).thenReturn(Optional.of(userEntity));
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userDao.merge(argumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> userOptional = userServices.authenticate("random-name", "random-password");
        assertTrue(userOptional.isPresent());

        User authenticatedUser = userOptional.get();
        assertThat(authenticatedUser.getUsername(), is("random-name"));
        assertThat(authenticatedUser.getLinks().size(), is(1));
        assertThat(argumentCaptor.getValue().getLoginCounter(), is(0));
    }

    @Test
    public void shouldReturnEmptyAndIncrementLoginCount_ifAuthenticationFail() throws Exception {
        User user = aUser();
        user.setLoginCounter(1);
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setService(new ServiceEntity(user.getGatewayAccountId()));
        userEntity.setPassword("hashed-password");

        when(passwordHasher.isEqual("random-password", "hashed-password")).thenReturn(false);
        when(userDao.findByUsername("random-name")).thenReturn(Optional.of(UserEntity.from(user)));
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userDao.merge(argumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> userOptional = userServices.authenticate("random-name", "random-password");
        assertFalse(userOptional.isPresent());

        UserEntity savedUser = argumentCaptor.getValue();
        assertTrue(within(3, SECONDS, savedUser.getCreatedAt()).matches(savedUser.getUpdatedAt()));
        assertThat(savedUser.getLoginCounter(), is(2));
        assertThat(savedUser.isDisabled(), is(false));
    }

    @Test
    public void shouldLockUser_onTooManyAuthFailures() throws Exception {
        User user = aUser();
        user.setLoginCounter(2);
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setService(new ServiceEntity(user.getGatewayAccountId()));
        userEntity.setPassword("hashed-password");

        when(passwordHasher.isEqual("random-password", "hashed-password")).thenReturn(false);
        when(userDao.findByUsername("random-name")).thenReturn(Optional.of(UserEntity.from(user)));
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userDao.merge(argumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        userServices.authenticate("random-name", "random-password");
        UserEntity savedUser = argumentCaptor.getValue();
        assertTrue(within(3, SECONDS, savedUser.getCreatedAt()).matches(savedUser.getUpdatedAt()));
        assertThat(savedUser.getLoginCounter(), is(3));
        assertThat(savedUser.isDisabled(), is(true));

    }

    @Test
    public void shouldErrorWhenDisabled_evenIfUsernamePasswordMatches() throws Exception {
        User user = aUser();
        user.setDisabled(true);

        UserEntity userEntity = UserEntity.from(user);
        userEntity.setService(new ServiceEntity(user.getGatewayAccountId()));
        userEntity.setPassword("hashed-password");
        when(passwordHasher.isEqual("random-password", "hashed-password")).thenReturn(true);
        when(userDao.findByUsername("random-name")).thenReturn(Optional.of(userEntity));

        try {
            userServices.authenticate("random-name", "random-password");
            fail();
        } catch (WebApplicationException e) {
            assertThat(e.getResponse().getStatus(), is(401));
        }

    }

    @Test
    public void shouldIncreaseLoginCount_whenRecordLoginAttempt() throws Exception {
        User user = aUser();
        user.setLoginCounter(1);

        UserEntity userEntity = UserEntity.from(user);
        userEntity.setService(new ServiceEntity(user.getGatewayAccountId()));
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByUsername("random-name")).thenReturn(userEntityOptional);
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userDao.merge(argumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> userOptional = userServices.recordLoginAttempt("random-name");

        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getUsername(), is("random-name"));
        assertThat(userOptional.get().getLoginCounter(), is(2));
        assertThat(userOptional.get().isDisabled(), is(false));

        UserEntity savedUser = argumentCaptor.getValue();
        assertTrue(within(3, SECONDS, savedUser.getCreatedAt()).matches(savedUser.getUpdatedAt()));
    }

    @Test
    public void shouldLockAccount_whenRecordLoginAttempt_ifMoreThanAllowedLoginAttempts() throws Exception {
        User user = aUser();
        user.setLoginCounter(3);

        UserEntity userEntity = UserEntity.from(user);
        userEntity.setService(new ServiceEntity(user.getGatewayAccountId()));
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByUsername("random-name")).thenReturn(userEntityOptional);
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userDao.merge(argumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        userServices.recordLoginAttempt("random-name");
        UserEntity savedUser = argumentCaptor.getValue();
        assertTrue(within(3, SECONDS, savedUser.getCreatedAt()).matches(savedUser.getUpdatedAt()));
        assertThat(savedUser.getLoginCounter(), is(4));
        assertThat(savedUser.isDisabled(), is(true));

    }

    @Test
    public void shouldReturnEmpty_whenRecordLoginAttempt_ifUserNotFound() throws Exception {
        when(userDao.findByUsername("random-name")).thenReturn(Optional.empty());
        Optional<User> userOptional = userServices.recordLoginAttempt("random-name");

        assertFalse(userOptional.isPresent());
    }

    @Test
    public void shouldReturnUser_whenResetLoginAttempt_ifUserFound() throws Exception {
        User user = aUser();

        UserEntity userEntity = UserEntity.from(user);
        userEntity.setService(new ServiceEntity(user.getGatewayAccountId()));
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByUsername("random-name")).thenReturn(userEntityOptional);
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userDao.merge(argumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> userOptional = userServices.resetLoginAttempts("random-name");
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getUsername(), is("random-name"));
        assertThat(userOptional.get().getLoginCounter(), is(0));
        assertThat(userOptional.get().isDisabled(), is(false));

        UserEntity savedUser = argumentCaptor.getValue();
        assertTrue(within(3, SECONDS, savedUser.getCreatedAt()).matches(savedUser.getUpdatedAt()));
    }

    @Test
    public void shouldReturnEmpty_whenResetLoginAttempt_ifUserNotFound() throws Exception {
        when(userDao.findByUsername("random-name")).thenReturn(Optional.empty());
        Optional<User> userOptional = userServices.resetLoginAttempts("random-name");

        assertFalse(userOptional.isPresent());
    }

    @Test
    public void shouldReturnUser_whenIncrementingSessionVersion_ifUserFound() throws Exception {
        User user = aUser();

        JsonNode node = new ObjectMapper().valueToTree(ImmutableMap.of("path", "sessionVersion", "op", "append", "value", "2"));
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setService(new ServiceEntity(user.getGatewayAccountId()));
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByUsername("random-name")).thenReturn(userEntityOptional);

        Optional<User> userOptional = userServices.patchUser("random-name", PatchRequest.from(node));
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getUsername(), is("random-name"));
        assertThat(userOptional.get().getSessionVersion(), is(2));
    }

    @Test
    public void shouldReturnUser_withDisabled_ifUserFoundDuringPatch() throws Exception {
        User user = aUser();

        JsonNode node = new ObjectMapper().valueToTree(ImmutableMap.of("path", "disabled", "op", "replace", "value", "true"));
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setService(new ServiceEntity(user.getGatewayAccountId()));
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByUsername("random-name")).thenReturn(userEntityOptional);

        assertFalse(user.isDisabled());

        Optional<User> userOptional = userServices.patchUser("random-name", PatchRequest.from(node));
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getUsername(), is("random-name"));
        assertTrue(userOptional.get().isDisabled());
    }

    private User aUser() {
        return User.from("random-name", "random-password", "random@email.com", "1", "784rh", "8948924");
    }

}
