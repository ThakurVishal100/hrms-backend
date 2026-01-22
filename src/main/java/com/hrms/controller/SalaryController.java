package com.hrms.controller;

import com.hrms.service.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salary")
@CrossOrigin(origins = "http://localhost:5173")
public class SalaryController {

    @Autowired
    private SalaryService salaryService;

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadSalarySlip(
            @RequestParam Integer empId,
            @RequestParam String month,
            @RequestParam int year) {

//        byte[] is a raw binary representation of data used to store or transfer files like PDFs, images, and videos in memory.
        byte[] pdfBytes = salaryService.generateSalarySlip(empId, month, year);

        //  CONTENT_DISPOSITION  :- browser ko batata hai pdf ko download krna h display nhi
        //  attachment  :-  auto download trigger hota hai
        // filename :-  download hone wali file ka naam

        //   .contentType(MediaType.APPLICATION_PDF)  :-  browser ko batata hai ki pdf file hai

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=SalarySlip_" + month + "_" + year + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}