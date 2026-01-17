package com.rssb.application.service;

import com.rssb.application.dto.SewadarFormSubmissionRequest;
import com.rssb.application.dto.SewadarFormSubmissionResponse;
import com.rssb.application.entity.Program;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.entity.SewadarFormSubmission;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.ProgramRepository;
import com.rssb.application.repository.SewadarFormSubmissionRepository;
import com.rssb.application.repository.SewadarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SewadarFormSubmissionService {

    private final SewadarFormSubmissionRepository formSubmissionRepository;
    private final ProgramRepository programRepository;
    private final SewadarRepository sewadarRepository;

    public SewadarFormSubmissionResponse submitForm(SewadarFormSubmissionRequest request, Long sewadarId) {
        Program program = programRepository.findById(request.getProgramId())
            .orElseThrow(() -> new ResourceNotFoundException("Program", "id", request.getProgramId()));

        Sewadar sewadar = sewadarRepository.findByZonalId(sewadarId)
            .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", sewadarId));

        // Check if form already submitted
        SewadarFormSubmission existing = formSubmissionRepository
            .findByProgramAndSewadar(program, sewadar)
            .orElse(null);

        SewadarFormSubmission submission;
        if (existing != null) {
            // Update existing
            existing.setName(request.getName());
            existing.setStartingDateTimeFromHome(request.getStartingDateTimeFromHome());
            existing.setReachingDateTimeToHome(request.getReachingDateTimeToHome());
            existing.setOnwardTrainFlightDateTime(request.getOnwardTrainFlightDateTime());
            existing.setOnwardTrainFlightNo(request.getOnwardTrainFlightNo());
            existing.setReturnTrainFlightDateTime(request.getReturnTrainFlightDateTime());
            existing.setReturnTrainFlightNo(request.getReturnTrainFlightNo());
            existing.setStayInHotel(request.getStayInHotel());
            existing.setStayInPandal(request.getStayInPandal());
            submission = formSubmissionRepository.save(existing);
        } else {
            // Create new
            submission = SewadarFormSubmission.builder()
                .program(program)
                .sewadar(sewadar)
                .name(request.getName())
                .startingDateTimeFromHome(request.getStartingDateTimeFromHome())
                .reachingDateTimeToHome(request.getReachingDateTimeToHome())
                .onwardTrainFlightDateTime(request.getOnwardTrainFlightDateTime())
                .onwardTrainFlightNo(request.getOnwardTrainFlightNo())
                .returnTrainFlightDateTime(request.getReturnTrainFlightDateTime())
                .returnTrainFlightNo(request.getReturnTrainFlightNo())
                .stayInHotel(request.getStayInHotel())
                .stayInPandal(request.getStayInPandal())
                .build();
            submission = formSubmissionRepository.save(submission);
        }

        log.info("Form submitted for program {} by sewadar {}", program.getId(), sewadarId);
        return mapToResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<SewadarFormSubmissionResponse> getSubmissionsForProgram(Long programId) {
        List<SewadarFormSubmission> submissions = formSubmissionRepository.findByProgramId(programId);
        return submissions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SewadarFormSubmissionResponse getSubmissionForSewadar(Long programId, Long sewadarId) {
        SewadarFormSubmission submission = formSubmissionRepository
            .findByProgramIdAndSewadarZonalId(programId, sewadarId)
            .orElse(null);

        if (submission == null) {
            return null;
        }

        return mapToResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<SewadarFormSubmissionResponse> getPendingFormsForSewadar(Long sewadarId) {
        // Get all programs where form is released but sewadar hasn't submitted
        // This will be handled in the controller by checking workflow status
        return List.of();
    }

    private SewadarFormSubmissionResponse mapToResponse(SewadarFormSubmission submission) {
        return SewadarFormSubmissionResponse.builder()
            .id(submission.getId())
            .programId(submission.getProgram().getId())
            .programTitle(submission.getProgram().getTitle())
            .sewadarId(submission.getSewadar().getZonalId())
            .sewadarName(submission.getSewadar().getFirstName() + " " + submission.getSewadar().getLastName())
            .name(submission.getName())
            .startingDateTimeFromHome(submission.getStartingDateTimeFromHome())
            .reachingDateTimeToHome(submission.getReachingDateTimeToHome())
            .onwardTrainFlightDateTime(submission.getOnwardTrainFlightDateTime())
            .onwardTrainFlightNo(submission.getOnwardTrainFlightNo())
            .returnTrainFlightDateTime(submission.getReturnTrainFlightDateTime())
            .returnTrainFlightNo(submission.getReturnTrainFlightNo())
            .stayInHotel(submission.getStayInHotel())
            .stayInPandal(submission.getStayInPandal())
            .submittedAt(submission.getSubmittedAt())
            .build();
    }
}

