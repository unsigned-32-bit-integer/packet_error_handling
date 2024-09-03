package uk.u32.packeterrorhandling.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Debug(export = true)
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin extends SimpleChannelInboundHandler<Packet<?>> {
    @Final
    @Shadow
    private static Logger LOGGER;

    @Shadow
    public abstract void exceptionCaught(ChannelHandlerContext context, Throwable ex);

    @Inject(
            method = "sendInternal",
            at = @At(
                    value = "INVOKE",
                    target = "io/netty/channel/ChannelFuture.addListener (Lio/netty/util/concurrent/GenericFutureListener;)Lio/netty/channel/ChannelFuture;",
                    ordinal = 1,
                    remap = false
            ),
            cancellable = true
    )
    private void replaceDefaultExceptionListener(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush, CallbackInfo ci, @Local(ordinal = 0) ChannelFuture channelFuture) {
        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                handleException(packet, future.cause());
            }
        });
        ci.cancel();
    }

    @Unique
    public void handleException(Packet<?> packet, Throwable ex) {
        if (packet.isWritingErrorSkippable()) {
            if (!LOGGER.isDebugEnabled()) {
                LOGGER.warn(String.format("Failed to send packet %s, skipping", packet.getPacketId()));
            } else {
                LOGGER.warn(String.format("Failed to send packet %s, skipping", packet.getPacketId()), ex);
            }
        } else {
            this.exceptionCaught(null, ex);
        }
    }

}