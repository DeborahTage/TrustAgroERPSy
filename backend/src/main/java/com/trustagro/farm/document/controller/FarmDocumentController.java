package com.trustagro.farm.document.controller;

import com.trustagro.farm.document.service.FarmDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/farm-documents")
@RequiredArgsConstructor
public class FarmDocumentController {

    private final FarmDocumentService farmDocumentService;

    @GetMapping(value = "/{type}/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAuthority('farm:read') or hasAuthority('chickensale:read')")
    public ResponseEntity<String> render(@PathVariable String type, @PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(farmDocumentService.render(type, id));
    }
}
