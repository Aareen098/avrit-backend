package com.hireflow.interview;

// What a candidate may see about their own interview. Staff notes are never included.
public record MyInterviewDto(Long id, String reference, String jobTitle, String company,
                             String date, String time, String type, String interviewer) {

    public static MyInterviewDto from(Interview i) {
        var a = i.getApplication();
        return new MyInterviewDto(i.getId(), a.getReference(), a.getJob().getTitle(), a.getJob().getCompany().getName(),
                i.getInterviewDate().toString(), i.getInterviewTime().toString(), i.getType(), i.getInterviewer());
    }
}