package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

/**
 * Represents an element in a wired network.
 */
public interface IWiredElement extends IWiredSender
{
    /**
     * The peripherals this network element provides.
     *
     * @return The peripherals this node provides. This is only called when initially
     * attaching to a network and after a call to {@link IWiredNode#invalidate()}}, so
     * one does not need to cache it.
     * @see IWiredNode#invalidate()
     */
    @Nonnull
    default Map<String, IPeripheral> getPeripherals()
    {
        return Collections.emptyMap();
    }

    /**
     * Called when various elements on the network change.
     *
     * @param change The change which occurred.
     */
    default void networkChanged( @Nonnull INetworkChange change )
    {
    }
}
