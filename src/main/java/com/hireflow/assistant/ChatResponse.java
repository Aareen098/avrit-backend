package com.hireflow.assistant;

import com.hireflow.application.ApplicationStage;
import java.util.List;

// What the chat page shows: the answer text plus a small card for each candidate it mentions.
public record ChatResponse(String answer, List<Card> candidates) {

    public record Card(Long id, String name, String job, Integer aiScore, ApplicationStage stage) {
    }
}