package com.kovix.infiniteoneblocks;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells the client how long the line is, so it can predict mining of normally unbreakable line blocks.
 */
public record LineInfoPayload(int length) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LineInfoPayload> TYPE = new CustomPacketPayload.Type<>(InfiniteOneBlocks.id("line_info"));
	public static final StreamCodec<ByteBuf, LineInfoPayload> CODEC = ByteBufCodecs.VAR_INT.map(LineInfoPayload::new, LineInfoPayload::length);

	@Override
	public CustomPacketPayload.Type<LineInfoPayload> type() {
		return TYPE;
	}
}
