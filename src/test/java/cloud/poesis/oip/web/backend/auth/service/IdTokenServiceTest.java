package cloud.poesis.oip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.poesis.oip.web.backend.auth.entity.Account;
import cloud.poesis.oip.web.backend.auth.model.AuthenticationRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class IdTokenServiceTest {

  @Mock private AuthenticationManager authenticationManager;

  @Mock private AccountService accountService;

  @InjectMocks private IdTokenService service;

  @Test
  void shouldIdentifyAccountAfterCredentialVerification() {
    AuthenticationRequest request =
        AuthenticationRequest.builder().email("user@oip.local").password("secret").build();
    Account account = Account.builder().id(UUID.randomUUID()).email("user@oip.local").build();

    when(accountService.loadAccountByEmail("user@oip.local")).thenReturn(account);

    Account identified = service.identify(request);

    verify(authenticationManager)
        .authenticate(new UsernamePasswordAuthenticationToken("user@oip.local", "secret"));
    verify(accountService).loadAccountByEmail("user@oip.local");
    assertThat(identified).isEqualTo(account);
  }
}
