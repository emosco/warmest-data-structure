package com.example.controller;

import com.example.api.WarmestDataStructureInterface;
import com.example.dto.PutRequest;
import com.example.exception.WarmestNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/warmest")
public class WarmestDataStructureController {

    private final WarmestDataStructureInterface service;

    public WarmestDataStructureController(WarmestDataStructureInterface service) {
        this.service = service;
    }

    @PutMapping("/{key}")
    @Operation(summary = "Put a string key and its associated integer value", description = "Put a string key and its associated integer value into the WarmestDataStructure.")
    public ResponseEntity<Integer> put(@PathVariable String key, @Valid @RequestBody PutRequest putRequest) {
        Integer prevValue = service.put(key, putRequest.value());
        if (prevValue == null){
            return ResponseEntity.status(HttpStatus.CREATED).body(null);
        }
        return ResponseEntity.ok(prevValue);
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Deletes a string key and its associated integer value", description = "Deletes a string key and its associated integer value from the WarmestDataStructure.")
    public ResponseEntity<Integer> delete(@PathVariable String key) {
        Integer prevValue = service.remove(key);
        return ResponseEntity.ok(prevValue);
    }

    @GetMapping("/{key}")
    @Operation(summary = "Gets the associated integer value of the input param key", description = "Gets the associated integer value of the input param key from the WarmestDataStructure.")
    public ResponseEntity<Integer> get(@PathVariable String key) {
        Integer value = service.get(key);
        return ResponseEntity.ok(value);
    }

    @GetMapping
    @Operation(summary = "Gets the warmest key", description = "Gets the warmest (most recent used) key from the WarmestDataStructure.")
    public ResponseEntity<String> getWarmest() {
        String warmest = service.getWarmest();
        if (warmest == null) {
            throw new WarmestNotFoundException();
        }
        return ResponseEntity.ok(warmest);
    }


}
