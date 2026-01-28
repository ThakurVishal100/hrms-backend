package com.hrms.service;

import com.hrms.entity.Employee;
import com.hrms.entity.EmployeeBankDetails;
import com.hrms.entity.EmployeeSalary;
import com.hrms.entity.User; // Imported User
import com.hrms.repository.EmployeeBankDetailsRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.EmployeeSalaryRepository;
import com.hrms.repository.UserRepository; // Imported UserRepository

import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Map;

@Service
public class SalaryService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeSalaryRepository salaryRepository;

    @Autowired
    private EmployeeBankDetailsRepository bankRepository;

    // ADDED: To fetch the logged-in user correctly
    @Autowired
    private UserRepository userRepository;

    private static final Color HEADER_BG_COLOR = Color.BLACK;
    private static final Color HEADER_TEXT_COLOR = Color.WHITE;
    private static final Color LABEL_BG_COLOR = new Color(225, 225, 225); // Light Gray
    private static final Color BORDER_COLOR = Color.GRAY;

    public byte[] generateSalarySlip(Integer targetEmpId, String month, int year, String loggedInLoginId) {

        if (month == null || month.trim().isEmpty()) {
            throw new IllegalArgumentException("Month cannot be empty");
        }

        Month selectedMonth;
        try {
            selectedMonth = Month.valueOf(month.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid month provided"+ month);
        }

        YearMonth selectedYearMonth = YearMonth.of(year, selectedMonth);
        YearMonth currentYearMonth = YearMonth.now(ZoneId.of("Asia/Kolkata"));

        //  Future / current month restriction
        if (!selectedYearMonth.isBefore(currentYearMonth)) {
            throw new IllegalArgumentException(
                    "Salary slip for current or future month is not generated yet. Please wait until the month ends."
            );
        }

        // 1. Fetch Target Employee
        Employee targetEmployee = employeeRepository.findById(targetEmpId)
                .orElseThrow(() -> new RuntimeException("Target Employee not found"));

        // 2. Fetch Requester from USER Table (FIXED)
        User requesterUser = userRepository.findByLoginId(loggedInLoginId)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));

        // Extract details from User entity safely
        Integer reqRole = requesterUser.getRole().getRoleId();

        // Handle cases where User might not be linked to Employee or Company (e.g. Super Admin)
        Integer reqEmpId = (requesterUser.getEmployee() != null) ? requesterUser.getEmployee().getEmployeeId() : -1;
        Integer reqCompanyId = (requesterUser.getCompany() != null) ? requesterUser.getCompany().getCompanyId() : -1;

        Integer targetId = targetEmployee.getEmployeeId();

        // ACCESS CONTROL LOGIC (Preserved)
        if (reqEmpId.equals(targetId)) {
            // Case A: Self-Download -> ALLOWED
        } else if (reqRole == 1) {
            // Case B: Super Admin -> ALLOWED (Global Access)
        } else if (reqRole == 2) {
            // Case C: HR Manager -> RESTRICTED (Same Company Only)
            Integer targetCompanyId = targetEmployee.getCompany().getCompanyId();

            if (!reqCompanyId.equals(targetCompanyId)) {
                throw new RuntimeException("ACCESS DENIED: You cannot access employees of another company.");
            }
        } else {
            // Case D: Regular Employee trying to download others -> DENIED
            throw new RuntimeException("ACCESS DENIED: You do not have permission.");
        }

        // Fetch Salary Data
        EmployeeSalary salary = salaryRepository.findByEmployee_EmployeeIdAndStatus(targetEmpId, 1)
                .orElseThrow(() -> new RuntimeException("Salary details not found for this employee"));

        EmployeeBankDetails bank = bankRepository.findByEmployee_EmployeeId(targetEmpId)
                .orElseThrow(()-> new RuntimeException("Employee Bank Details not found"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 20, 20, 20, 20); // Small margins
            PdfWriter.getInstance(document, out);
            document.open();

            //  FONTS
            Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font addressFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, HEADER_TEXT_COLOR);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            // 1. COMPANY HEADER (Top Right)
            Paragraph companyName = new Paragraph(targetEmployee.getCompany().getCompanyName(), companyFont);
            companyName.setAlignment(Element.ALIGN_RIGHT);
            document.add(companyName);

            Paragraph companyAddr = new Paragraph("B-67, Takshilla Colony, 1st Floor, Garh Road, Meerut, UP\nTel.: +91-123-4567890", addressFont);
            companyAddr.setAlignment(Element.ALIGN_RIGHT);
            companyAddr.setSpacingAfter(15);
            document.add(companyAddr);

            // 2. SLIP TITLE BAR (Black Strip)
            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);
            PdfPCell titleCell = new PdfPCell(new Phrase("PAYSLIP FOR " + month.toUpperCase() + " " + year, sectionHeaderFont));
            titleCell.setBackgroundColor(HEADER_BG_COLOR);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setPadding(5);
            titleCell.setBorderColor(BORDER_COLOR);
            titleTable.addCell(titleCell);
            document.add(titleTable);

            // 3. EMPLOYEE DETAILS GRID (4 Columns)
            PdfPTable empTable = new PdfPTable(4);
            empTable.setWidthPercentage(100);
            empTable.setWidths(new float[]{2f, 3f, 2f, 3f}); // Col widths

            // Row 1
            addLabelCell(empTable, "Name", labelFont);
            addValueCell(empTable, targetEmployee.getEmployeeName(), valueFont);
            addLabelCell(empTable, "PAN", labelFont);
            addValueCell(empTable, bank.getPanNumber() != null ? bank.getPanNumber() : "NA", valueFont);

            // Row 2
            addLabelCell(empTable, "Employee Code", labelFont);
            addValueCell(empTable, "EMP" + targetEmployee.getEmployeeId(), valueFont);
            addLabelCell(empTable, "Gender", labelFont);
            addValueCell(empTable, "Male", valueFont);

            // Row 3
            addLabelCell(empTable, "Department", labelFont);
            addValueCell(empTable, targetEmployee.getDepartment() != null ? targetEmployee.getDepartment().getDeptName() : "-", valueFont);
            addLabelCell(empTable, "Bank Name", labelFont);
            addValueCell(empTable, bank.getBankName() != null ? bank.getBankName() : "NA", valueFont);

            // Row 4
            addLabelCell(empTable, "Date of Birth", labelFont);
            addValueCell(empTable, "01-Jan-1990", valueFont); // Placeholder
            addLabelCell(empTable, "IFSC Code", labelFont);
            addValueCell(empTable, bank.getIfscCode() != null ? bank.getIfscCode() : "NA", valueFont);

            // Row 5
            addLabelCell(empTable, "Designation", labelFont);
            addValueCell(empTable, targetEmployee.getDesignation() != null ? targetEmployee.getDesignation().getDesignationName() : "-", valueFont);
            addLabelCell(empTable, "Account Number", labelFont);
            addValueCell(empTable, bank.getAccountNumber() != null ? bank.getAccountNumber() : "NA", valueFont);

            // Row 6
            addLabelCell(empTable, "Location", labelFont);
            addValueCell(empTable, "Meerut", valueFont);
            addLabelCell(empTable, "PF Account Number", labelFont);
            addValueCell(empTable, bank.getPfAccountNumber() != null ? bank.getPfAccountNumber() : "NA", valueFont);

            // Row 7
            addLabelCell(empTable, "Joining Date", labelFont);
            addValueCell(empTable, String.valueOf(targetEmployee.getJoiningDate()), valueFont);
            addLabelCell(empTable, "PF UAN", labelFont);
            addValueCell(empTable, bank.getUanNumber() != null ? bank.getUanNumber() : "NA", valueFont);

            // Row 8
            addLabelCell(empTable, "Leaving Date", labelFont);
            addValueCell(empTable, targetEmployee.getRelievingDate() != null ? String.valueOf(targetEmployee.getRelievingDate()) : "", valueFont);
            addLabelCell(empTable, "ESI Number", labelFont);
            addValueCell(empTable, bank.getEsiNumber() != null ? bank.getEsiNumber() : "NA", valueFont);

            // Row 9
            addLabelCell(empTable, "Tax Regime", labelFont);
            addValueCell(empTable, "NA", valueFont);
            addLabelCell(empTable, "", labelFont); // Empty Filler
            addValueCell(empTable, "", valueFont);

            document.add(empTable);
            document.add(new Paragraph("\n")); // Spacer

            // 4. ATTENDANCE / DAYS STRIP
            PdfPTable daysHeader = new PdfPTable(3);
            daysHeader.setWidthPercentage(100);
            addSectionHeader(daysHeader, "PAY DAYS", sectionHeaderFont);
            addSectionHeader(daysHeader, "ATTENDANCE ARREAR DAYS", sectionHeaderFont);
            addSectionHeader(daysHeader, "INCREMENT ARREAR DAYS", sectionHeaderFont);
            document.add(daysHeader);

            PdfPTable daysValues = new PdfPTable(3);
            daysValues.setWidthPercentage(100);
            addCenteredCell(daysValues, "30.00", valueFont);
            addCenteredCell(daysValues, "0.00", valueFont);
            addCenteredCell(daysValues, "0.00", valueFont);
            document.add(daysValues);
            document.add(new Paragraph("\n"));

            // 5. EARNINGS TABLE
            // Header
            PdfPTable earningsHeader = new PdfPTable(1);
            earningsHeader.setWidthPercentage(100);
            PdfPCell earnTitle = new PdfPCell(new Phrase("EARNINGS (INR)", sectionHeaderFont));
            earnTitle.setBackgroundColor(HEADER_BG_COLOR);
            earnTitle.setHorizontalAlignment(Element.ALIGN_CENTER);
            earnTitle.setPadding(4);
            earningsHeader.addCell(earnTitle);
            document.add(earningsHeader);

            // Columns: Component, Rate, Monthly, Arrear, Total
            PdfPTable earningsTable = new PdfPTable(5);
            earningsTable.setWidthPercentage(100);
            earningsTable.setWidths(new float[]{3f, 1.5f, 1.5f, 1.5f, 1.5f});

            addGrayHeader(earningsTable, "COMPONENTS", labelFont);
            addGrayHeader(earningsTable, "RATE", labelFont);
            addGrayHeader(earningsTable, "MONTHLY", labelFont);
            addGrayHeader(earningsTable, "ARREAR", labelFont);
            addGrayHeader(earningsTable, "TOTAL", labelFont);

            BigDecimal totalEarnings = BigDecimal.ZERO;

            Map<String, Object> breakup = salary.getSalaryBreakup();
            if (breakup != null) {
                for (Map.Entry<String, Object> entry : breakup.entrySet()) {
                    String key = entry.getKey();
                    if(isDeduction(key)) continue;

                    BigDecimal val = new BigDecimal(String.valueOf(entry.getValue()));
                    totalEarnings = totalEarnings.add(val);

                    addContentCell(earningsTable, key, valueFont); // Component
                    addContentCell(earningsTable, formatVal(val), valueFont); // Rate (Mocked as same)
                    addContentCell(earningsTable, formatVal(val), valueFont); // Monthly
                    addContentCell(earningsTable, "0.00", valueFont); // Arrear
                    addContentCell(earningsTable, formatVal(val), valueFont); // Total
                }
            }
            // Total Row
            addGrayHeader(earningsTable, "TOTAL EARNINGS", labelFont);
            addContentCell(earningsTable, formatVal(totalEarnings), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9));
            addContentCell(earningsTable, formatVal(totalEarnings), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9));
            addContentCell(earningsTable, "0.00", valueFont);
            addContentCell(earningsTable, formatVal(totalEarnings), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9));

            document.add(earningsTable);
            document.add(new Paragraph("\n"));

            // 6. DEDUCTIONS TABLE
            PdfPTable dedHeader = new PdfPTable(1);
            dedHeader.setWidthPercentage(100);
            PdfPCell dedTitle = new PdfPCell(new Phrase("DEDUCTIONS (INR)", sectionHeaderFont));
            dedTitle.setBackgroundColor(HEADER_BG_COLOR);
            dedTitle.setHorizontalAlignment(Element.ALIGN_CENTER);
            dedTitle.setPadding(4);
            dedHeader.addCell(dedTitle);
            document.add(dedHeader);

            PdfPTable dedTable = new PdfPTable(2);
            dedTable.setWidthPercentage(100);
            dedTable.setWidths(new float[]{4f, 2f}); // Component, Total

            addGrayHeader(dedTable, "COMPONENTS", labelFont);
            addGrayHeader(dedTable, "TOTAL", labelFont);

            BigDecimal totalDeductions = BigDecimal.ZERO;
            if (breakup != null) {
                for (Map.Entry<String, Object> entry : breakup.entrySet()) {
                    String key = entry.getKey();
                    if(!isDeduction(key)) continue; // Only Deductions

                    BigDecimal val = new BigDecimal(String.valueOf(entry.getValue()));
                    totalDeductions = totalDeductions.add(val);

                    addContentCell(dedTable, key, valueFont);
                    addContentCell(dedTable, formatVal(val), valueFont);
                }
            }
            // Filler if empty
            if(totalDeductions.compareTo(BigDecimal.ZERO) == 0) {
                addContentCell(dedTable, "PF", valueFont); addContentCell(dedTable, "NILL", valueFont);
                addContentCell(dedTable, "IT", valueFont); addContentCell(dedTable, "NILL", valueFont);
            }

            addGrayHeader(dedTable, "TOTAL DEDUCTIONS", labelFont);
            addContentCell(dedTable, totalDeductions.compareTo(BigDecimal.ZERO) == 0 ? "NILL" : formatVal(totalDeductions), labelFont);
            document.add(dedTable);
            document.add(new Paragraph("\n"));

            // 7. NET PAY
            BigDecimal netPay = totalEarnings.subtract(totalDeductions);

            PdfPTable netTable = new PdfPTable(2);
            netTable.setWidthPercentage(100);
            netTable.setWidths(new float[]{1.5f, 4f});

            addLabelCell(netTable, "NET PAY (INR)", labelFont);
            addValueCell(netTable, formatVal(netPay), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10));

            addLabelCell(netTable, "NET PAY IN WORDS", labelFont);
            addValueCell(netTable, convertToWords(netPay.longValue()), valueFont);
            document.add(netTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    //  HELPER METHODS

    private void addLabelCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(LABEL_BG_COLOR);
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addValueCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addSectionHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BG_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addCenteredCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addGrayHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(LABEL_BG_COLOR); // Light Gray for Table Sub-headers
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addContentCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private boolean isDeduction(String key) {
        String k = key.toLowerCase();
        return k.contains("tax") || k.contains("pf") || k.contains("esi") || k.contains("loan") || k.contains("deduction");
    }

    private String formatVal(BigDecimal val) {
        return String.format("%.2f", val);
    }

    private static final String[] units = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] tens = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public String convertToWords(long n) {
        if (n == 0) return "Zero";
        return convert(n) + " Only";
    }

    private String convert(long n) {
        if (n < 0) return "Minus " + convert(-n);
        if (n < 20) return units[(int) n];
        if (n < 100) return tens[(int) n / 10] + ((n % 10 != 0) ? " " : "") + units[(int) n % 10];
        if (n < 1000) return units[(int) n / 100] + " Hundred" + ((n % 100 != 0) ? " " : "") + convert(n % 100);
        if (n < 100000) return convert(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " : "") + convert(n % 1000);
        if (n < 10000000) return convert(n / 100000) + " Lakh" + ((n % 100000 != 0) ? " " : "") + convert(n % 100000);
        return convert(n / 10000000) + " Crore" + ((n % 10000000 != 0) ? " " : "") + convert(n % 10000000);
    }
}