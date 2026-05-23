import java.util.*;
import java.text.SimpleDateFormat;

// ─────────────────────────────────────────────
//  Transaction Record
// ─────────────────────────────────────────────
class Transaction {
    private String type;
    private double amount;
    private double balanceAfter;
    private String date;

    public Transaction(String type, double amount, double balanceAfter) {
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
    }

    @Override
    public String toString() {
        return String.format("  [%s]  %-12s  Amount: %+10.2f  |  Balance: %10.2f",
                date, type, amount, balanceAfter);
    }
}

// ─────────────────────────────────────────────
//  Bank Account
// ─────────────────────────────────────────────
class BankAccount {
    private String accountNumber;
    private String accountHolder;
    private String pin;
    private double balance;
    private String accountType;   // SAVINGS / CURRENT
    private List<Transaction> transactions = new ArrayList<>();
    private boolean isLocked = false;
    private int failedAttempts = 0;

    public BankAccount(String accountNumber, String accountHolder,
                       String pin, double initialDeposit, String accountType) {
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.pin           = pin;
        this.balance       = initialDeposit;
        this.accountType   = accountType;
        transactions.add(new Transaction("ACCOUNT OPEN", initialDeposit, balance));
    }

    // ── getters ──────────────────────────────
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolder() { return accountHolder; }
    public String getAccountType()   { return accountType;   }
    public double getBalance()       { return balance;       }
    public boolean isLocked()        { return isLocked;      }

    // ── PIN verification ─────────────────────
    public boolean verifyPin(String inputPin) {
        if (isLocked) return false;
        if (pin.equals(inputPin)) {
            failedAttempts = 0;
            return true;
        }
        failedAttempts++;
        if (failedAttempts >= 3) {
            isLocked = true;
            System.out.println("\n  ⚠  Account LOCKED after 3 failed PIN attempts!");
        }
        return false;
    }

    // ── Operations ───────────────────────────
    public boolean deposit(double amount) {
        if (amount <= 0) return false;
        balance += amount;
        transactions.add(new Transaction("DEPOSIT", +amount, balance));
        return true;
    }

    public boolean withdraw(double amount) {
        if (amount <= 0 || amount > balance) return false;
        balance -= amount;
        transactions.add(new Transaction("WITHDRAW", -amount, balance));
        return true;
    }

    public boolean transferTo(BankAccount target, double amount) {
        if (amount <= 0 || amount > balance) return false;
        balance -= amount;
        transactions.add(new Transaction("TRANSFER OUT → " + target.getAccountNumber(), -amount, balance));
        target.receiveTransfer(this, amount);
        return true;
    }

    public void receiveTransfer(BankAccount from, double amount) {
        balance += amount;
        transactions.add(new Transaction("TRANSFER IN ← " + from.getAccountNumber(), +amount, balance));
    }

    public void printStatement() {
        System.out.println("\n  ══════════════════════════════════════════════════════════════════");
        System.out.printf ("  ACCOUNT STATEMENT  |  %s  |  %s%n", accountNumber, accountHolder);
        System.out.println("  ══════════════════════════════════════════════════════════════════");
        for (Transaction t : transactions) System.out.println(t);
        System.out.printf ("%n  Current Balance: ₹ %.2f%n", balance);
        System.out.println("  ══════════════════════════════════════════════════════════════════");
    }
}

// ─────────────────────────────────────────────
//  Bank (manages all accounts)
// ─────────────────────────────────────────────
class Bank {
    private String bankName;
    private Map<String, BankAccount> accounts = new LinkedHashMap<>();
    private int accountCounter = 1001;

    public Bank(String bankName) { this.bankName = bankName; }

    public String getBankName() { return bankName; }

    // ── Create account ───────────────────────
    public BankAccount createAccount(String holderName, String pin,
                                     double initialDeposit, String type) {
        String accNo = "ACC" + (accountCounter++);
        BankAccount acc = new BankAccount(accNo, holderName, pin, initialDeposit, type);
        accounts.put(accNo, acc);
        return acc;
    }

    // ── Find account ─────────────────────────
    public BankAccount findAccount(String accNo) {
        return accounts.get(accNo.toUpperCase());
    }

    // ── Admin: list all accounts ─────────────
    public void listAllAccounts() {
        if (accounts.isEmpty()) { System.out.println("  No accounts found."); return; }
        System.out.println("\n  ┌──────────────┬──────────────────────┬───────────┬─────────────────┐");
        System.out.println("  │ Account No   │ Holder               │ Type      │ Balance         │");
        System.out.println("  ├──────────────┼──────────────────────┼───────────┼─────────────────┤");
        for (BankAccount a : accounts.values()) {
            System.out.printf("  │ %-12s │ %-20s │ %-9s │ ₹ %12.2f  │%n",
                    a.getAccountNumber(), a.getAccountHolder(),
                    a.getAccountType(), a.getBalance());
        }
        System.out.println("  └──────────────┴──────────────────────┴───────────┴─────────────────┘");
    }
}

// ─────────────────────────────────────────────
//  Main Console Application
// ─────────────────────────────────────────────
public class BankingSystem {

    static Scanner sc   = new Scanner(System.in);
    static Bank    bank = new Bank("JavaBank");

    // ══ Entry Point ══════════════════════════
    public static void main(String[] args) {
        // Pre-load two demo accounts (PIN: 1234 / 5678)
        bank.createAccount("Alice Johnson", "1234", 50000, "SAVINGS");
        bank.createAccount("Bob Smith",    "5678", 20000, "CURRENT");

        header("Welcome to " + bank.getBankName());
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = intInput("Enter choice: ");
            switch (choice) {
                case 1 -> openAccount();
                case 2 -> login();
                case 3 -> { adminPanel(); }
                case 0 -> { running = false; System.out.println("\n  Thank you for using " + bank.getBankName() + ". Goodbye!\n"); }
                default -> System.out.println("  ✗  Invalid option.\n");
            }
        }
    }

    // ══ Menus ═════════════════════════════════
    static void printMainMenu() {
        System.out.println("  ┌─────────────────────────────┐");
        System.out.println("  │         MAIN MENU           │");
        System.out.println("  ├─────────────────────────────┤");
        System.out.println("  │  1. Open New Account        │");
        System.out.println("  │  2. Login to Account        │");
        System.out.println("  │  3. Admin Panel             │");
        System.out.println("  │  0. Exit                    │");
        System.out.println("  └─────────────────────────────┘");
    }

    static void printAccountMenu(String name) {
        System.out.println("\n  ┌─────────────────────────────┐");
        System.out.printf ("  │  Hello, %-19s │%n", name + "!");
        System.out.println("  ├─────────────────────────────┤");
        System.out.println("  │  1. Check Balance           │");
        System.out.println("  │  2. Deposit Money           │");
        System.out.println("  │  3. Withdraw Money          │");
        System.out.println("  │  4. Transfer Funds          │");
        System.out.println("  │  5. View Statement          │");
        System.out.println("  │  0. Logout                  │");
        System.out.println("  └─────────────────────────────┘");
    }

    // ══ Features ══════════════════════════════

    // ── 1. Open Account ──────────────────────
    static void openAccount() {
        header("Open New Account");
        String name    = strInput("Full Name        : ");
        String type    = accountTypeInput();
        double deposit = doubleInput("Initial Deposit  : ₹ ");
        if (deposit < 500) { System.out.println("  ✗  Minimum deposit is ₹500.\n"); return; }
        String pin     = pinInput("Set 4-digit PIN  : ");
        String confirm = pinInput("Confirm PIN      : ");
        if (!pin.equals(confirm)) { System.out.println("  ✗  PINs do not match.\n"); return; }

        BankAccount acc = bank.createAccount(name, pin, deposit, type);
        System.out.println("\n  ✔  Account created successfully!");
        System.out.println("  ▶  Your Account Number: " + acc.getAccountNumber());
        System.out.printf ("  ▶  Opening Balance    : ₹ %.2f%n%n", deposit);
    }

    // ── 2. Login ─────────────────────────────
    static void login() {
        header("Account Login");
        String accNo = strInput("Account Number: ").toUpperCase();
        BankAccount acc = bank.findAccount(accNo);
        if (acc == null) { System.out.println("  ✗  Account not found.\n"); return; }
        if (acc.isLocked()) { System.out.println("  ✗  Account is locked. Contact admin.\n"); return; }

        String pin = pinInput("Enter PIN     : ");
        if (!acc.verifyPin(pin)) {
            System.out.println("  ✗  Incorrect PIN.\n");
            return;
        }

        // ── Account Session ──
        boolean session = true;
        while (session) {
            printAccountMenu(acc.getAccountHolder().split(" ")[0]);
            int ch = intInput("Enter choice: ");
            switch (ch) {
                case 1 -> System.out.printf("%n  ✔  Current Balance: ₹ %.2f%n%n", acc.getBalance());
                case 2 -> deposit(acc);
                case 3 -> withdraw(acc);
                case 4 -> transfer(acc);
                case 5 -> acc.printStatement();
                case 0 -> { session = false; System.out.println("\n  Logged out successfully.\n"); }
                default -> System.out.println("  ✗  Invalid option.\n");
            }
        }
    }

    static void deposit(BankAccount acc) {
        double amt = doubleInput("\n  Deposit Amount: ₹ ");
        if (acc.deposit(amt))
            System.out.printf("  ✔  ₹ %.2f deposited. New Balance: ₹ %.2f%n%n", amt, acc.getBalance());
        else
            System.out.println("  ✗  Invalid amount.\n");
    }

    static void withdraw(BankAccount acc) {
        double amt = doubleInput("\n  Withdraw Amount: ₹ ");
        if (acc.withdraw(amt))
            System.out.printf("  ✔  ₹ %.2f withdrawn. New Balance: ₹ %.2f%n%n", amt, acc.getBalance());
        else
            System.out.println("  ✗  Invalid amount or insufficient funds.\n");
    }

    static void transfer(BankAccount acc) {
        String targetNo = strInput("\n  Transfer to Account No: ").toUpperCase();
        BankAccount target = bank.findAccount(targetNo);
        if (target == null)               { System.out.println("  ✗  Target account not found.\n"); return; }
        if (target == acc)                { System.out.println("  ✗  Cannot transfer to same account.\n"); return; }
        double amt = doubleInput("  Transfer Amount: ₹ ");
        if (acc.transferTo(target, amt))
            System.out.printf("  ✔  ₹ %.2f transferred to %s. Balance: ₹ %.2f%n%n",
                    amt, target.getAccountHolder(), acc.getBalance());
        else
            System.out.println("  ✗  Invalid amount or insufficient funds.\n");
    }

    // ── 3. Admin Panel ───────────────────────
    static void adminPanel() {
        header("Admin Panel");
        String pwd = strInput("Admin Password: ");
        if (!pwd.equals("admin123")) { System.out.println("  ✗  Incorrect admin password.\n"); return; }
        System.out.println("\n  ✔  Admin access granted.");
        bank.listAllAccounts();
        System.out.println();
    }

    // ══ Helpers ════════════════════════════════
    static void header(String title) {
        System.out.println("\n  ════════════════════════════════════");
        System.out.printf ("       %s%n", title);
        System.out.println("  ════════════════════════════════════");
    }

    static String strInput(String prompt) {
        System.out.print("  " + prompt);
        return sc.nextLine().trim();
    }

    static int intInput(String prompt) {
        System.out.print("  " + prompt);
        try { int v = Integer.parseInt(sc.nextLine().trim()); return v; }
        catch (NumberFormatException e) { return -1; }
    }

    static double doubleInput(String prompt) {
        System.out.print("  " + prompt);
        try { return Double.parseDouble(sc.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    static String pinInput(String prompt) {
        System.out.print("  " + prompt);
        String pin = sc.nextLine().trim();
        // Mask display (console can't truly hide, but we validate length)
        if (!pin.matches("\\d{4}")) {
            System.out.println("  ✗  PIN must be exactly 4 digits.");
            return pinInput(prompt);
        }
        return pin;
    }

    static String accountTypeInput() {
        System.out.println("  Account Type: 1. Savings  2. Current");
        int ch = intInput("Choose (1/2): ");
        return ch == 2 ? "CURRENT" : "SAVINGS";
    }
}