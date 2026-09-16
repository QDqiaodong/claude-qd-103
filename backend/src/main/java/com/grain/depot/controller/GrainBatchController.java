package com.grain.depot.controller;

import com.grain.depot.entity.GrainBatch;
import com.grain.depot.service.GrainBatchService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GrainBatchController {

    private final GrainBatchService service;

    public GrainBatchController(GrainBatchService service) {
        this.service = service;
    }

    @GetMapping("/batches")
    public List<GrainBatch> list(@RequestParam(required = false) Long granaryId,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String variety) {
        return service.list(granaryId, status, variety);
    }

    @PostMapping("/batches")
    public GrainBatch create(@RequestBody GrainBatch input) {
        return service.create(input);
    }

    @PutMapping("/batches/{id}")
    public GrainBatch update(@PathVariable Long id, @RequestBody GrainBatch input) {
        return service.update(id, input);
    }
}
