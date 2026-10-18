package com.hireflow.interview;

// Same field names the Interviews page already uses. date is "2026-10-12", time is "11:00".
public record InterviewDto(
        Long id,
        Long candidateId,
        String candidateName,
        String job,
        String date,
        String time,
        String type,
        String interviewer,
        String notes) {

    public static InterviewDto from(Interview i) {
        return new InterviewDto(
                i.getId(), i.getApplication().getId(), i.getApplication().getName(),
                i.getApplication().getJob().getTitle(),
                i.getInterviewDate().toString(), i.getInterviewTime().toString(),
                i.getType(), i.getInterviewer(), i.getNotes() == null ? "" : i.getNotes());
    }
}