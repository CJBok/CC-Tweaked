package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.network.IPacketSender;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

public interface IWiredElement extends IPacketSender
{
    IWiredNode getNode();

    void setNode( IWiredNode node );

    @Nonnull
    default Map<String, IPeripheral> getPeripherals()
    {
        return Collections.emptyMap();
    }

    default void networkChanged( INetworkChange change )
    {
    }
}
