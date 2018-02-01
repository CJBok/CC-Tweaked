package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.network.IPacketSender;

import javax.annotation.Nonnull;

public interface IWiredSender extends IPacketSender
{
    /**
     * The node in the network representing this object.
     *
     * This should be used as a proxy for the main network. One should send packets
     * and register receivers through this object.
     *
     * @return The node for this element.
     */
    @Nonnull
    IWiredNode getNode();
}
