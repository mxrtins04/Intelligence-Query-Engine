package com.mxr.integration.controller;

import org.springframework.web.bind.annotation.RestController;

import com.mxr.integration.Response.MultipleProcessedResponse;
import com.mxr.integration.Response.PersonExistsResponse;
import com.mxr.integration.Response.PersonSummary;
import com.mxr.integration.Response.ProcessedResponse;
import com.mxr.integration.model.Person;
import com.mxr.integration.request.NewEntityRequest;
import com.mxr.integration.service.IntegrationService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class classifyController {
    private final IntegrationService integrationService;

    public classifyController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping("/")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    @PostMapping("/api/profiles")
    public ResponseEntity<ProcessedResponse> savePerson(@Valid @RequestBody NewEntityRequest request) {
        String name = request.getName();
        ProcessedResponse response = integrationService.savePerson(name);
        if(response instanceof PersonExistsResponse) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/api/profiles/{id}")
    public ProcessedResponse getUserById(@PathVariable UUID id) {

        Person person = integrationService.getPersonById(id);

        return mapToProcessedResponse(person);
    }

    @GetMapping("/api/profiles")
    public MultipleProcessedResponse getUsersByParams(@RequestParam(required = false) String gender,
            @RequestParam(required = false) 
            String countryId, @RequestParam(required = false) String ageGroup,
            @RequestParam(required = false) Integer minimumAge,
            @RequestParam(required = false) Integer maximumAge,
            @RequestParam(required = false) Double countryProbability,
            @RequestParam(required = false) Double genderProbability) {
        List<PersonSummary> response = integrationService.searchPeople(gender, countryId, ageGroup, minimumAge, maximumAge, countryProbability, genderProbability);
        return mapSpecToMultipleProcessedResponse(response);
    }

    @DeleteMapping("/api/profiles/{id}")
    public ResponseEntity<String> deleteUserById(@PathVariable UUID id) {
        integrationService.deletePersonById(id);
        return ResponseEntity.noContent().build();
    }


    private ProcessedResponse mapToProcessedResponse(Person person) {
        return ProcessedResponse.builder()
                .status("success")
                .data(person)
                .build();
    }
    

    
    private MultipleProcessedResponse mapSpecToMultipleProcessedResponse(List<PersonSummary> list) {

        return MultipleProcessedResponse.builder()
                .status("success")
                .count(list.size())
                .data(list)
                .build();
    }

}
