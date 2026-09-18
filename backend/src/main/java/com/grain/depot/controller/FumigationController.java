package com.grain.depot.controller;

import com.grain.depot.entity.Fumigation;
import com.grain.depot.service.FumigationService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FumigationController {

    private final FumigationService service;

    public FumigationController(FumigationService service) {
        this.service = service;
    }

    @GetMapping("/fumigations")
    public List<Fumigation> list(@RequestParam(required = false) Long granaryId,
                                 @RequestParam(required = false) String status) {
        return service.list(granaryId, status);
    }

    @PostMapping("/fumigations")
    public Fumigation create(@RequestBody Fumigation input) {
        return service.create(input);
    }

    @PostMapping("/fumigations/{id}/start")
    public Fumigation start(@PathVariable Long id) {
        return service.start(id);
    }

    @PostMapping("/fumigations/{id}/release")
    public Fumigation release(@PathVariable Long id, @RequestBody Fumigation input) {
        return service.release(id, input);
    }
}
