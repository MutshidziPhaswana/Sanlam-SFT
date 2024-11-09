import java.util.List;
import java.util.Optional;

public interface BankAccountRepository {
    Optional<BankAccount> findByAccountId(Long id);
    List<BankAccount> findAllAccounts();
    Optional<BigDecimal> findBalanceByAccountId(Long accountId);
    void updateBalance(Long accountId, BigDecimal amount, Long version);
}
