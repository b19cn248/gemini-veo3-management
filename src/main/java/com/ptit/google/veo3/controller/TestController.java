package com.ptit.google.veo3.controller;

import com.ptit.google.veo3.common.response.ApiResponse;
import com.ptit.google.veo3.common.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/response")
    public ResponseEntity<ApiResponse<Map<String, String>>> testGenericResponse() {
        Map<String, String> testData = new HashMap<>();
        testData.put("message", "Generic response is working!");
        testData.put("status", "success");
        
        return ResponseUtil.ok("Test successful", testData);
    }

    @GetMapping("/error")
    public ResponseEntity<ApiResponse<String>> testErrorHandling() {
        throw new IllegalArgumentException("This is a test error");
    }
}