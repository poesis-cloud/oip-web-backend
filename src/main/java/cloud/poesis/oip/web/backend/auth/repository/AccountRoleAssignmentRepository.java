package cloud.poesis.oip.web.backend.auth.repository;

import cloud.poesis.oip.web.backend.auth.entity.AccountRoleAssignment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRoleAssignmentRepository
    extends JpaRepository<AccountRoleAssignment, UUID> {}
