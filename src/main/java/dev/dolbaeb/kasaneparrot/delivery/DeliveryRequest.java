package dev.dolbaeb.kasaneparrot.delivery;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Неизменяемый запрос на доставку: кто, что, кому и куда.
 */
public record DeliveryRequest(
        @NotNull UUID senderId,
        @NotNull String senderName,
        @NotNull UUID targetId,
        @NotNull String targetName,
        @NotNull ItemStack payload,
        @NotNull DeliveryDestination destination,
        long createdAtMillis
) {
}
