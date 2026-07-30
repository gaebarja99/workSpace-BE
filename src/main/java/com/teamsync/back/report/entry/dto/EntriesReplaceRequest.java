package com.teamsync.back.report.entry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** PUT /api/reports/me/entries?weekStart=&section= 요청 바디. */
public record EntriesReplaceRequest(
		@NotNull List<@Valid EntryUpsertRequest> entries
) {
}
