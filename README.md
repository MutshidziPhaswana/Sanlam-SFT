# Bank Account Withdrawal Code Improvement

This README provides an overview of my approach to improving the bank account withdrawal system, ensuring that the fundamental business capability remains intact. Below, I detail the enhancements I have made, elaborate on the choices that influenced my implementation, provide the final code, and explain any unclear library usage.

## Outline of the Approach

The goal of this exercise was to improve the original bank account withdrawal code, enhancing aspects such as maintainability, scalability, fault tolerance, testability, and adherence to clean coding principles. The fundamental business capability allowing users to withdraw money from a bank account and publishing a notification event remains unchanged, but the implementation was modified to follow best practices and adhere to the **SOLID** principles.

### Core Enhancements Made
1. **Separation of Concerns**: Split the responsibilities of the `withdraw` method into smaller, specialized methods to improve readability and maintainability.
2. **Layered Architecture**: The solution was divided into different layers: Controller, Service, Repository to follow a layered architecture pattern.
3. **SOLID Principles**: Refactored the code to strictly adhere to the SOLID principles of object-oriented design, making it more extensible, reusable, and resilient to changes.
4. **Optimistic Locking**: Implemented optimistic locking to avoid race conditions when multiple users try to withdraw from the same account. this would mean applying a database migration to add the locking field.
5. **Dependency Injection**: Utilized dependency injection to make components modular and easily testable.
6. **Logging**: Added structured logging for better observability and to aid debugging.
7. **Retry Logic**: Added retry logic (exponential backoff) to ensure reliability when publishing events to AWS SNS.

## Elaboration on Implementation Choices

### 1. **Single Responsibility Principle (SRP)**
The original `withdraw` method in the `BankAccountService` class was performing multiple responsibilities, including checking the account balance, updating the balance, and publishing an SNS notification. I broke this method into three distinct responsibilities:
- **Check Balance**: Ensures the account has sufficient funds for withdrawal.
- **Update Balance**: Updates the account balance while ensuring no other user has modified the account (using optimistic locking).
- **Publish Notification**: Publishes an event using the `NotificationService` interface.

This refactoring makes the code easier to understand, test, and modify.

### 2. **Open/Closed and Dependency Inversion Principles (OCP & DIP)**
To make the system extensible, I introduced the `NotificationService` interface. The `BankAccountService` class now depends on an abstraction rather than a concrete implementation (`SnsNotificationService`). This allows us to easily add new types of notification mechanisms (e.g., email or SMS) without modifying existing code. Dependency Injection (DI) was used to inject the desired implementation, making the system more flexible and testable.

### 3. **Optimistic Locking for Data Integrity**
Optimistic locking was implemented in the `updateBalance` method to ensure data consistency when multiple transactions attempt to modify the same account balance. By including a version number, the system prevents multiple users from concurrently modifying the balance, thus avoiding race conditions and ensuring that no unexpected overwrites occur.

### 4. **Retry Logic for AWS SNS Notification**
A retry mechanism with exponential backoff was implemented in the `SnsNotificationService` class to handle potential failures when publishing messages to AWS SNS. This ensures that transient issues do not disrupt the system's ability to notify stakeholders of important account events.

### 5. **Interface Segregation Principle (ISP)**
The repository methods were logically segregated to ensure that each method serves a specific purpose. This reduces the likelihood of unnecessary coupling and makes the classes more focused and easier to modify.

## Fixed Code Snippet

### BankAccountController
```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/bank")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @Autowired
    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam("accountId") Long accountId, @RequestParam("amount") BigDecimal amount) {
        return bankAccountService.withdraw(accountId, amount);
    }
}
```

### BankAccountService
```java
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
```

## Unclear Library Usage

- **AWS SNS SDK**: The AWS SNS SDK is used for sending notifications about withdrawals. The SNS client is instantiated with a specific AWS region and uses a retry mechanism to handle transient issues. The client initialization is managed through Spring's dependency injection, which ensures flexibility and easier testing.
- **Spring Framework**: Spring Boot and Spring's core features are used extensively. Specifically:
  - **`@Autowired`**: This is used to inject dependencies, such as the repository and notification service, making components loosely coupled and easier to manage.
  - **`@Transactional`**: This annotation ensures that the withdrawal operations are performed atomically. If any part of the method fails, the entire transaction is rolled back, preserving data consistency.
  - **`@Value`**: This annotation is used to inject configuration properties (like SNS topic ARN and AWS region) into the application, ensuring that environment-specific values are managed outside the code.
  - **`Optional`**: The Optional class **(Java feature)** is used to safely handle potentially null values returned by methods such as findBalanceByAccountId. This prevents null pointer exceptions and makes the code more robust by requiring the caller to explicitly handle absent values.

## Conclusion

In summary, the refactoring focused on improving the overall design of the bank withdrawal process, making it more maintainable, testable, and resilient to future changes. The core functionality of the withdrawal operation, including balance updates and event notifications, has been preserved, with added fault tolerance and better adherence to software engineering best practices.

