package com.trustagro.farm.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.farm.entity.EnvironmentRecord;
import com.trustagro.farm.repository.EnvironmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
public class IoTController {

    private final EnvironmentRecordRepository environmentRepository;

    @PostMapping("/ingest")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<Void>> ingestSensorData(@RequestBody EnvironmentRecord record) {
        record.setTimestamp(LocalDateTime.now());
        record.setDataSource("IOT");
        environmentRepository.save(record);
        
        // In real system, we'd trigger real-time alerts via WebSockets here if thresholds are exceeded
        
        return ResponseEntity.ok(ApiResponse.success("Data ingested", null));
    }
}
