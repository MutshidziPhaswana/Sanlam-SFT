import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class BankAccountService {

    private static final Logger logger = LoggerFactory.getLogger(BankAccountService.class);

    private final BankAccountRepository bankAccountRepository;
    private final NotificationService notificationService;

    @Autowired
    public BankAccountService(BankAccountRepository bankAccountRepository, NotificationService notificationService) {
        this.bankAccountRepository = bankAccountRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public String withdraw(Long accountId, BigDecimal amount) {
        logger.info("Attempting to withdraw {} from account {}", amount, accountId);

        if (isBalanceSufficient(accountId, amount)) {
            updateBalance(accountId, amount);
            publishWithdrawalEvent(accountId, amount, "SUCCESSFUL");
            return "Withdrawal successful";
        } else {
            logger.warn("Insufficient funds for withdrawal from account {}", accountId);
            return "Insufficient funds for withdrawal";
        }
    }

    private boolean isBalanceSufficient(Long accountId, BigDecimal amount) {
        Optional<BigDecimal> currentBalance = bankAccountRepository.findBalanceByAccountId(accountId);
        return currentBalance.isPresent() && currentBalance.get().compareTo(amount) >= 0;
    }

    private void updateBalance(Long accountId, BigDecimal amount) {
        Optional<BankAccount> bankAccountOpt = bankAccountRepository.findByAccountId(accountId);
        if (bankAccountOpt.isPresent()) {
            BankAccount bankAccount = bankAccountOpt.get();
            bankAccountRepository.updateBalance(accountId, amount, bankAccount.getVersion());
            logger.info("Balance updated successfully for account {}", accountId);
        }
    }

    private void publishWithdrawalEvent(Long accountId, BigDecimal amount, String status) {
        WithdrawalEvent event = new WithdrawalEvent(amount, accountId, status);
        notificationService.sendNotification(event);
        logger.info("Withdrawal event published successfully for account {}", accountId);
    }
}
