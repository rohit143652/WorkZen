package com.example.application.mail_module.dto;

/** One line of the overview table: who sends as whom. {@code clientCompanyId} null = the platform default row. */
public record MailSettingsOverviewRow(Long clientCompanyId, String companyName,
                                      boolean hasOwnSettings, boolean ownEnabled,
                                      String effectiveSource, String effectiveFromAddress,
                                      boolean usable, String problem) {
}
