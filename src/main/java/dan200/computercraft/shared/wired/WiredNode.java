package dan200.computercraft.shared.wired;

import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNetwork;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

public final class WiredNode implements IWiredNode
{
    private final IWiredElement provider;
    WiredNetwork network;

    Map<String, IPeripheral> peripherals = Collections.emptyMap();

    private WiredNode( IWiredElement provider )
    {
        this.provider = provider;
    }

    public static IWiredNode create( IWiredElement element )
    {
        WiredNode node = new WiredNode( element );
        node.network = new WiredNetwork( node );
        return node;
    }

    @Nonnull
    @Override
    public IWiredNetwork getNetwork()
    {
        return network;
    }

    boolean updatePeripherals()
    {
        Map<String, IPeripheral> oldPeripherals = peripherals;
        Map<String, IPeripheral> newPeripherals = provider.getPeripherals();

        peripherals = newPeripherals;
        return !oldPeripherals.equals( newPeripherals );
    }

    @Override
    public String toString()
    {
        return "WiredNode{" + provider + "}";
    }
}
