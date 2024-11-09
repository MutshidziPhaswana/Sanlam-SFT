import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.dao.OptimisticLockingFailureException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcBankAccountRepository implements BankAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcBankAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<BankAccount> rowMapper = (rs, rowNum) -> {
        // Maps each row of data from the ResultSet to a BankAccount object
        BankAccount bankAccount = new BankAccount();

         // Set fields in BankAccount based on ResultSet columns
        bankAccount.setAccountId(rs.getLong("id"));
        bankAccount.setBalance(rs.getBigDecimal("balance"));
        bankAccount.setVersion(rs.getLong("version"));
        return bankAccount;
    };

    @Override
    public List<BankAccount> findAllAccounts() {
        String sql = "SELECT * FROM accounts";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public Optional<BankAccount> findByAccountId(Long accountId) {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        List<BankAccount> results = jdbcTemplate.query(sql, rowMapper, accountId);
        return results.stream().findFirst();
    }

    @Override
    public Optional<BigDecimal> findBalanceByAccountId(Long accountId) {
        String sql = "SELECT balance FROM accounts WHERE id = ?";
        return jdbcTemplate.query(sql, rs -> {
            return rs.next() ? Optional.of(rs.getBigDecimal("balance")) : Optional.empty();
        }, accountId);
    }
    
    @Override
    public void updateBalance(Long accountId, BigDecimal amount, Long version) {
        String sql = "UPDATE accounts SET balance = balance - ?, version = version + 1 WHERE id = ? AND version = ?";
        int rowsAffected = jdbcTemplate.update(sql, amount, accountId, version);

        //If the update operation affects zero rows, it means that the record's version number has changed (modified by another transaction).
        if (rowsAffected == 0) {
            throw new OptimisticLockingFailureException("Failed to update balance for account " + accountId + ". The account may have been modified concurrently.");
        }
    }
    
}
