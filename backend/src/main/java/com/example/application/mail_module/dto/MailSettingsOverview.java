package com.example.application.mail_module.dto;

import java.util.List;

public record MailSettingsOverview(MailSettingsOverviewRow platform, List<MailSettingsOverviewRow> companies) {
}
