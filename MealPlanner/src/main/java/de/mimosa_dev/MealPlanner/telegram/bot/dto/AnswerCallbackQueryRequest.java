package de.mimosa_dev.MealPlanner.telegram.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AnswerCallbackQueryRequest(@JsonProperty("callback_query_id") String callbackQueryId, String text) {
}
