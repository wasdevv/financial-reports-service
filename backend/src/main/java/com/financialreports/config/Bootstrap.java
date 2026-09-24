package com.financialreports.config;

import com.financialreports.entity.FinancialRecord;
import com.financialreports.entity.Report;
import com.financialreports.entity.User;
import com.financialreports.repository.ReportRepository;
import com.financialreports.repository.UserRepository;
import com.financialreports.service.ReportService;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro público cria ANALYST, então o primeiro ADMIN vem do ambiente (ADMIN_EMAIL/ADMIN_PASSWORD).
 * DEMO_SEED=true popula relatórios de exemplo, de um dono com senha aleatória: dá o que ver
 * na demo pública sem publicar credencial nenhuma.
 */
@Component
public class Bootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

    private final AppProperties props;
    private final UserRepository users;
    private final ReportRepository reports;
    private final PasswordEncoder encoder;

    public Bootstrap(AppProperties props, UserRepository users, ReportRepository reports, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.reports = reports;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = props.admin().email();
        String password = props.admin().password();
        if (email != null && !email.isBlank()) {
            if (password == null || password.length() < 12) {
                throw new IllegalStateException("ADMIN_PASSWORD must be at least 12 characters");
            }
            User admin = users.findByEmailIgnoreCase(email).orElseGet(() -> {
                User u = new User();
                u.setEmail(email);
                u.setUsername("admin");
                u.setFullName("Administrator");
                u.setPassword(encoder.encode(password));
                return u;
            });
            admin.setRole(User.Role.ADMIN);
            admin.setActive(true);
            users.save(admin);
        }
        if (props.demoSeed() && reports.count() == 0) {
            seedDemo();
        }
    }

    private void seedDemo() {
        User owner = new User();
        owner.setUsername("demo-analyst");
        owner.setEmail("demo-analyst@example.invalid");
        owner.setFullName("Demo Analyst");
        byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);
        owner.setPassword(encoder.encode(Base64.getEncoder().encodeToString(random)));
        users.save(owner);

        report(owner, "Retail store, Q1", "2026-Q1", Report.Status.APPROVED,
                in("Sales", "84200.00"), in("Services", "12300.00"),
                out("Payroll", "41800.00"), out("Rent", "12000.00"), out("Inventory", "19650.40"));
        report(owner, "Retail store, Q2", "2026-Q2", Report.Status.APPROVED,
                in("Sales", "71900.00"), in("Services", "9800.00"),
                out("Payroll", "43100.00"), out("Rent", "12000.00"), out("Inventory", "14320.75"));
        report(owner, "Logistics branch, April", "2026-04", Report.Status.APPROVED,
                in("Freight contracts", "38500.00"),
                out("Fuel", "22900.00"), out("Fleet maintenance", "9400.00"), out("Payroll", "15600.00"));
        report(owner, "Logistics branch, May", "2026-05", Report.Status.APPROVED,
                in("Freight contracts", "29800.00"),
                out("Fuel", "41200.00"), out("Fleet maintenance", "13100.00"), out("Payroll", "15600.00"));
        report(owner, "Marketing pilot, June", "2026-06", Report.Status.APPROVED,
                out("Ads", "18400.00"), out("Agency fee", "6000.00"));
        report(owner, "Consulting unit, Q3", "2026-Q3", Report.Status.PENDING,
                in("Retainers", "56000.00"), in("Workshops", "8400.00"),
                out("Payroll", "39800.00"), out("Travel", "4100.00"), out("Software", "2300.00"));
        log.info("Demo data seeded");
    }

    private void report(User owner, String title, String period, Report.Status status, FinancialRecord... records) {
        Report r = new Report();
        r.setOwner(owner);
        r.setTitle(title);
        r.setPeriod(period);
        r.setStatus(status);
        r.setDescription("Sample data generated for the public demo.");
        int day = 1;
        for (FinancialRecord f : records) {
            f.setReport(r);
            f.setRecordDate(firstDay(period).plusDays(day++ * 3L));
            r.getRecords().add(f);
        }
        ReportService.recalculate(r);
        reports.save(r);
    }

    private static LocalDate firstDay(String period) {
        int year = Integer.parseInt(period.substring(0, 4));
        String rest = period.substring(5);
        int month = rest.startsWith("Q") ? (rest.charAt(1) - '1') * 3 + 1 : Integer.parseInt(rest);
        return LocalDate.of(year, month, 1);
    }

    private static FinancialRecord in(String category, String amount) {
        return record(FinancialRecord.Type.INCOME, category, amount);
    }

    private static FinancialRecord out(String category, String amount) {
        return record(FinancialRecord.Type.EXPENSE, category, amount);
    }

    private static FinancialRecord record(FinancialRecord.Type type, String category, String amount) {
        FinancialRecord f = new FinancialRecord();
        f.setType(type);
        f.setCategory(category);
        f.setAmount(new BigDecimal(amount));
        return f;
    }
}
