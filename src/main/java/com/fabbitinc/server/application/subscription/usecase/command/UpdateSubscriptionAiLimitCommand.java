package com.fabbitinc.server.application.subscription.usecase.command;

import java.math.BigDecimal;

public record UpdateSubscriptionAiLimitCommand(
        BigDecimal aiMonthlyCreditLimit,
        boolean aiHardLimitEnabled
) {
}
