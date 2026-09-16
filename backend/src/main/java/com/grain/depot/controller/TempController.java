package com.grain.depot.controller;

import com.grain.depot.entity.TempRecord;
import com.grain.depot.service.TempService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TempController {

    private final TempService service;

    public TempController(TempService service) {
        this.service = service;
    }

    @GetMapping("/temps")
    public List<TempRecord> list(
            @RequestParam(required = false) Long granaryId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDate) {
        return service.list(granaryId, recordDate);
    }

    @PostMapping("/temps")
    public TempRecord create(@RequestBody TempRecord input) {
        return service.create(input);
    }
}
