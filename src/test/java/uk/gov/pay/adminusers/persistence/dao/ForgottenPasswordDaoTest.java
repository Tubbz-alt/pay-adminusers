package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MINUTES;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

public class ForgottenPasswordDaoTest extends DaoTestBase {

    private UserDao userDao;
    private ForgottenPasswordDao forgottenPasswordDao;

    @Before
    public void before() throws Exception {
        userDao = env.getInstance(UserDao.class);
        forgottenPasswordDao = env.getInstance(ForgottenPasswordDao.class);
    }

    @Test
    public void shouldPersistAForgottenPasswordEntity() throws Exception {
        String random = newId();
        Integer randomInt = randomInt();
        String randomIntAsString = randomInt.toString();
        User user = User.from(randomInt, "user-" + random, "password" + random, random + "@example.com", Arrays.asList(randomIntAsString), randomIntAsString, "8395398535");
        UserEntity userEntity = UserEntity.from(user);
        userDao.persist(userEntity);

        ForgottenPassword forgottenPassword = ForgottenPassword.forgottenPassword("code-" + random, "user-" + random);

        ForgottenPasswordEntity forgottenPasswordEntity = ForgottenPasswordEntity.from(forgottenPassword, userEntity);
        forgottenPasswordDao.persist(forgottenPasswordEntity);

        assertThat(forgottenPasswordEntity.getId(), is(notNullValue()));
        List<Map<String, Object>> forgottenPasswordById = databaseTestHelper.findForgottenPasswordById(forgottenPasswordEntity.getId());

        assertThat(forgottenPasswordById.size(), is(1));
        assertThat(forgottenPasswordById.get(0).get("code"), is("code-" + random));

        Timestamp storedDate = (Timestamp) forgottenPasswordById.get(0).get("date");
        ZonedDateTime storedDateTime = ZonedDateTime.ofInstant(storedDate.toInstant(), ZoneId.of("UTC"));
        assertThat(storedDateTime, within(1, MINUTES, forgottenPassword.getDate()));
    }

    @Test
    public void shouldFindForgottenPasswordByCode_ifNotExpired() throws Exception {
        String random = newId();
        Integer randomInt = randomInt();
        String randomIntAsString = randomInt.toString();
        User user = User.from(randomInt, "user-" + random, "password" + random, random + "@example.com", Arrays.asList(randomIntAsString), randomIntAsString, "8395398535");
        UserEntity userEntity = UserEntity.from(user);
        userDao.persist(userEntity);

        ZonedDateTime notExpired = ZonedDateTime.now().minusMinutes(89);
        ForgottenPassword forgottenPassword = ForgottenPassword.forgottenPassword(randomInt, "code-" + random, "user-" + random, notExpired);

        databaseTestHelper.add(forgottenPassword, userEntity.getId());

        Optional<ForgottenPasswordEntity> forgottenPasswordEntityOptional = forgottenPasswordDao.findNonExpiredByCode(forgottenPassword.getCode());
        assertTrue(forgottenPasswordEntityOptional.isPresent());

        ForgottenPasswordEntity forgottenPasswordEntity = forgottenPasswordEntityOptional.get();
        assertThat(forgottenPasswordEntity.getCode(), is(forgottenPassword.getCode()));
        assertThat(forgottenPasswordEntity.getDate(), within(1, MINUTES, forgottenPassword.getDate()));
    }

    @Test
    public void shouldNotFindForgottenPasswordByCode_ifExpired() throws Exception {
        String random = newId();
        Integer randomInt = randomInt();
        String randomIntAsString = randomInt.toString();
        User user = User.from(randomInt, "user-" + random, "password" + random, random + "@example.com", Arrays.asList(randomIntAsString), randomIntAsString, "8395398535");
        UserEntity userEntity = UserEntity.from(user);
        userDao.persist(userEntity);

        ZonedDateTime expired = ZonedDateTime.now().minusMinutes(91);
        ForgottenPassword forgottenPassword = ForgottenPassword.forgottenPassword(randomInt, "code-" + random, "user-" + random, expired);

        databaseTestHelper.add(forgottenPassword, userEntity.getId());

        Optional<ForgottenPasswordEntity> forgottenPasswordEntityOptional = forgottenPasswordDao.findNonExpiredByCode(forgottenPassword.getCode());
        assertFalse(forgottenPasswordEntityOptional.isPresent());
    }

    @Test
    public void shouldRemoveForgottenPasswordEntity() {
        String random = newId();
        Integer randomInt = randomInt();
        String randomIntAsString = randomInt.toString();
        User user = User.from(randomInt, "user-" + random, "password" + random, random + "@example.com", Arrays.asList(randomIntAsString), randomIntAsString, "8395398535");
        UserEntity userEntity = UserEntity.from(user);
        userDao.persist(userEntity);

        ZonedDateTime notExpired = ZonedDateTime.now().minusMinutes(89);
        ForgottenPassword forgottenPassword = ForgottenPassword.forgottenPassword(randomInt, "code-" + random, "user-" + random, notExpired);

        databaseTestHelper.add(forgottenPassword, userEntity.getId());

        Optional<ForgottenPasswordEntity> forgottenPasswordEntityOptional = forgottenPasswordDao.findNonExpiredByCode(forgottenPassword.getCode());
        assertTrue(forgottenPasswordEntityOptional.isPresent());

        forgottenPasswordDao.remove(forgottenPasswordEntityOptional.get());

        assertThat(forgottenPasswordDao.findNonExpiredByCode(forgottenPassword.getCode()).isPresent(), is(false));
    }
}
