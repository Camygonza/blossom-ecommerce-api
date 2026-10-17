package com.blossom.ecommerce.auth.repository;

import com.blossom.ecommerce.auth.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	/** Bloqueo pesimista: dos /refresh simultáneos con el mismo token no pueden rotarlo a la vez. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update RefreshToken t set t.revoked = true where t.familyId = :familyId and t.revoked = false")
	int revokeFamily(@Param("familyId") String familyId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from RefreshToken t where t.expiresAt < :now")
	int deleteExpired(@Param("now") java.time.Instant now);

	long countByFamilyIdAndRevokedFalse(String familyId);

}
