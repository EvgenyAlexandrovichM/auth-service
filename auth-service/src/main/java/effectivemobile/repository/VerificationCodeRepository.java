package effectivemobile.repository;

import effectivemobile.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    Optional<VerificationCode> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<VerificationCode> findByEmailAndCodeAndUsedFalse(String email, String code);

}
