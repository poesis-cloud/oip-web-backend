package cloud.poesis.oip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cloud.poesis.oip.web.backend.auth.entity.Account;
import cloud.poesis.oip.web.backend.auth.entity.AccountRoleAssignment;
import cloud.poesis.oip.web.backend.auth.entity.Privilege;
import cloud.poesis.oip.web.backend.auth.entity.Role;
import cloud.poesis.oip.web.backend.auth.entity.RolePrivilegeAssignment;
import cloud.poesis.oip.web.backend.auth.repository.AccountRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock private AccountRepository accountRepository;

  @InjectMocks private AccountService accountService;

  @Test
  void loadUserByUsernameShouldReturnUserDetailsWithOnlyActiveAuthorities() {
    Privilege activePrivilege = Privilege.builder().id(UUID.randomUUID()).code("READ_USER").build();
    RolePrivilegeAssignment activeRolePrivilegeAssignment =
        RolePrivilegeAssignment.builder().id(UUID.randomUUID()).privilege(activePrivilege).build();

    Privilege revokedPrivilege =
        Privilege.builder().id(UUID.randomUUID()).code("DELETE_USER").build();
    RolePrivilegeAssignment revokedRolePrivilegeAssignment =
        RolePrivilegeAssignment.builder()
            .id(UUID.randomUUID())
            .privilege(revokedPrivilege)
            .unassignedAt(Instant.now())
            .build();

    Role activeRole =
        Role.builder()
            .id(UUID.randomUUID())
            .name("ADMIN")
            .rolePrivilegeAssignments(
                Set.of(activeRolePrivilegeAssignment, revokedRolePrivilegeAssignment))
            .build();

    AccountRoleAssignment activeRoleAssignment =
        AccountRoleAssignment.builder().id(UUID.randomUUID()).role(activeRole).build();

    AccountRoleAssignment expiredRoleAssignment =
        AccountRoleAssignment.builder()
            .id(UUID.randomUUID())
            .role(activeRole)
            .expiresAt(Instant.now().minusSeconds(300))
            .build();

    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("john.doe@oip.local")
            .passwordHash("hashed-password")
            .enabled(true)
            .accountRoleAssignments(Set.of(activeRoleAssignment, expiredRoleAssignment))
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("john.doe@oip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("john.doe@oip.local");

    assertThat(userDetails.getUsername()).isEqualTo("john.doe@oip.local");
    assertThat(userDetails.getPassword()).isEqualTo("hashed-password");
    assertThat(userDetails.isEnabled()).isTrue();
    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .containsExactlyInAnyOrder("READ_USER");
  }

  @Test
  void loadUserByUsernameShouldThrowWhenAccountNotFound() {
    when(accountRepository.findByEmailWithRolesAndPrivileges("missing@oip.local"))
        .thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> accountService.loadUserByUsername("missing@oip.local"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("missing@oip.local");
  }

  @Test
  void loadAccountByEmailShouldReturnAccountWhenExists() {
    Account account = Account.builder().email("admin@oip.local").build();
    when(accountRepository.findByEmail("admin@oip.local"))
        .thenReturn(java.util.Optional.of(account));

    Account loaded = accountService.loadAccountByEmail("admin@oip.local");
    assertThat(loaded.getEmail()).isEqualTo("admin@oip.local");
  }

  @Test
  void loadAccountByEmailShouldThrowWhenMissing() {
    when(accountRepository.findByEmail("missing@oip.local")).thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> accountService.loadAccountByEmail("missing@oip.local"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("missing@oip.local");
  }

  @Test
  void loadUserByUsernameShouldHandleNullAssignmentsAndDisabledAccount() {
    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("disabled@oip.local")
            .passwordHash("hashed-password")
            .enabled(false)
            .accountRoleAssignments(null)
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("disabled@oip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("disabled@oip.local");

    assertThat(userDetails.getUsername()).isEqualTo("disabled@oip.local");
    assertThat(userDetails.isEnabled()).isFalse();
    assertThat(userDetails.getAuthorities()).isEmpty();
  }

  @Test
  void loadUserByUsernameShouldIgnoreInvalidAuthoritiesAndKeepOnlyValidPrivilegeCodes() {
    Privilege validPrivilege = Privilege.builder().id(UUID.randomUUID()).code("READ_USER").build();
    Privilege nullCodePrivilege = Privilege.builder().id(UUID.randomUUID()).code(null).build();

    Role roleWithNullPrivilegeAssignments =
        Role.builder()
            .id(UUID.randomUUID())
            .name("NULL_PRIV_ASSIGNMENTS")
            .rolePrivilegeAssignments(null)
            .build();

    Role roleWithNullPrivilege =
        Role.builder()
            .id(UUID.randomUUID())
            .name("NULL_PRIVILEGE")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(null)
                        .build()))
            .build();

    Role roleWithNullPrivilegeCode =
        Role.builder()
            .id(UUID.randomUUID())
            .name("NULL_PRIVILEGE_CODE")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(nullCodePrivilege)
                        .build()))
            .build();

    Role roleWithValidPrivilege =
        Role.builder()
            .id(UUID.randomUUID())
            .name("VALID")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(validPrivilege)
                        .build()))
            .build();

    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("mixed@oip.local")
            .passwordHash("hashed-password")
            .enabled(true)
            .accountRoleAssignments(
                Set.of(
                    AccountRoleAssignment.builder().id(UUID.randomUUID()).role(null).build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(roleWithNullPrivilegeAssignments)
                        .build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(roleWithNullPrivilege)
                        .build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(roleWithNullPrivilegeCode)
                        .build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(roleWithValidPrivilege)
                        .build()))
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("mixed@oip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("mixed@oip.local");

    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .containsExactlyInAnyOrder("READ_USER");
  }

  @Test
  void loadUserByUsernameShouldIgnoreRevokedRoleAssignments() {
    Privilege privilege = Privilege.builder().id(UUID.randomUUID()).code("READ_USER").build();
    Role role =
        Role.builder()
            .id(UUID.randomUUID())
            .name("ADMIN")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(privilege)
                        .build()))
            .build();

    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("revoked@oip.local")
            .passwordHash("hashed-password")
            .enabled(true)
            .accountRoleAssignments(
                Set.of(
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(role)
                        .unassignedAt(Instant.now())
                        .build()))
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("revoked@oip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("revoked@oip.local");

    assertThat(userDetails.getAuthorities()).isEmpty();
  }

  @Test
  void loadUserByUsernameShouldIgnoreExpiredRoleAssignmentsEvenWhenNotRevoked() {
    Privilege privilege = Privilege.builder().id(UUID.randomUUID()).code("READ_USER").build();
    Role role =
        Role.builder()
            .id(UUID.randomUUID())
            .name("EXPIRED_ROLE")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(privilege)
                        .build()))
            .build();

    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("expired@oip.local")
            .passwordHash("hashed-password")
            .enabled(true)
            .accountRoleAssignments(
                Set.of(
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(role)
                        .expiresAt(Instant.now().minusSeconds(10))
                        .build()))
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("expired@oip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("expired@oip.local");

    assertThat(userDetails.getAuthorities()).isEmpty();
  }

  @Test
  void loadUserByUsernameShouldAcceptFutureExpiringRoleAssignments() {
    Privilege privilege = Privilege.builder().id(UUID.randomUUID()).code("READ_USER").build();
    Role role =
        Role.builder()
            .id(UUID.randomUUID())
            .name("FUTURE_ROLE")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(privilege)
                        .build()))
            .build();

    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("future@oip.local")
            .passwordHash("hashed-password")
            .enabled(true)
            .accountRoleAssignments(
                Set.of(
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(role)
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build()))
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("future@oip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("future@oip.local");

    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .containsExactlyInAnyOrder("READ_USER");
  }
}
