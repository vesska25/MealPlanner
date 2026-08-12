package de.mimosa_dev.MealPlanner.telegram.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record InlineKeyboardMarkup(@JsonProperty("inline_keyboard") List<List<InlineKeyboardButton>> inlineKeyboard) {

    public static InlineKeyboardMarkup singleRow(InlineKeyboardButton... buttons) {
        return new InlineKeyboardMarkup(List.of(List.of(buttons)));
    }
}
