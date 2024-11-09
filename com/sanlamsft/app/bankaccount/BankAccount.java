import java.math.BigDecimal;

public class BankAccount {

    private Long accountId;
    private BigDecimal balance = BigDecimal.ZERO; // to avoid potential null pointer exceptions
    private Long version; // Optimistic lock version number

    // Getters
    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    // Setters
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance : BigDecimal.ZERO; // Handle null balance
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // toString method
    @Override
    public String toString() {
        return "BankAccount{" +
                "accountId=" + accountId +
                ", balance=" + balance +
                ", version=" + version +
                '}';
    }
}
