package com.kovix.infiniteoneblocks.client.mixin;

import com.kovix.infiniteoneblocks.InfiniteOneBlocks;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * "Create New World" starts with the Infinite One Blocks world type selected; the others stay available.
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
	@ModifyArg(
		method = "openFresh(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldCallback;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;openCreateWorldScreen"),
		index = 4
	)
	private static ResourceKey<WorldPreset> infiniteoneblocks$defaultWorldType(final ResourceKey<WorldPreset> preset) {
		return InfiniteOneBlocks.WORLD_PRESET;
	}
}
