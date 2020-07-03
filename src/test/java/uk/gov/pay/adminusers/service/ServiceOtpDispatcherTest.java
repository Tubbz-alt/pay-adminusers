package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE;

@ExtendWith(MockitoExtension.class)
public class ServiceOtpDispatcherTest {

    @Mock
    private InviteDao inviteDao;
    @Mock
    private SecondFactorAuthenticator secondFactorAuthenticator;
    @Mock
    private NotificationService notificationService;

    private InviteOtpDispatcher serviceOtpDispatcher;

    @BeforeEach
    public void before() {
        serviceOtpDispatcher = new ServiceOtpDispatcher(inviteDao, secondFactorAuthenticator, notificationService);
    }

    @Test
    public void shouldSuccess_whenDispatchServiceOtp_ifInviteEntityExist() {
        String inviteCode = "valid-invite-code";
        String telephone = "+441134960000";
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setCode(inviteCode);
        inviteEntity.setType(InviteType.SERVICE);
        inviteEntity.setOtpKey("otp-key");
        inviteEntity.setTelephoneNumber(telephone);

        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(secondFactorAuthenticator.newPassCode("otp-key")).thenReturn(123456);
        when(notificationService.sendSecondFactorPasscodeSms(telephone, "123456", SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE))
                .thenReturn("success code from notify");
        boolean dispatched = serviceOtpDispatcher.dispatchOtp(inviteCode);

        assertThat(dispatched,is(true));
    }

    @Test
    public void shouldFail_whenDispatchServiceOtp_ifInviteEntityNotFound() {

        String inviteCode = "non-existent-code";
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.empty());

        boolean dispatched = serviceOtpDispatcher.dispatchOtp(inviteCode);

        assertThat(dispatched,is(false));
    }
}
