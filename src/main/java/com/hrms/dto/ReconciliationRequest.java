package com.hrms.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ReconciliationRequest {
    private MultipartFile file;
    private String whatsappText;
}