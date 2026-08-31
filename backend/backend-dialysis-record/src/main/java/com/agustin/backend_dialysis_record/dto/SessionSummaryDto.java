package com.agustin.backend_dialysis_record.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SessionSummaryDto {
    private int sessionsCount;
    private int totalInfusion;
    private int totalDrainage;
    private int totalBalance;
}
