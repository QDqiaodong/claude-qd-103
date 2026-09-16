package com.grain.depot.controller;

import com.grain.depot.entity.Granary;
import com.grain.depot.service.GranaryService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GranaryController {

    private final GranaryService service;

    public GranaryController(GranaryService service) {
        this.service = service;
    }

    @GetMapping("/granaries")
    public List<Granary> list(@RequestParam(required = false) String status,
                              @RequestParam(required = false) String keyword) {
        return service.list(status, keyword);
    }

    @PostMapping("/granaries")
    public Granary create(@RequestBody Granary input) {
        return service.create(input);
    }

    @PutMapping("/granaries/{id}")
    public Granary update(@PathVariable Long id, @RequestBody Granary input) {
        return service.update(id, input);
    }
}
