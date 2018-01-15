package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.Map;

public interface INetworkChange
{
    Map<String, IPeripheral> peripheralsAdded();

    Map<String, IPeripheral> peripheralsRemoved();
}
