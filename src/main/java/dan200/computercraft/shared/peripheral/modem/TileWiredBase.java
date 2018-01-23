package dan200.computercraft.shared.peripheral.modem;

import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.wired.IWiredElementTile;

import javax.annotation.Nonnull;
import java.util.Map;

public abstract class TileWiredBase extends TileModemBase implements IWiredElementTile
{
    @Override
    protected ModemPeripheral createPeripheral()
    {
        return new WiredModemPeripheral( this );
    }

    protected final WiredModemPeripheral getModem()
    {
        return (WiredModemPeripheral) m_modem;
    }

    protected final IWiredNode getNode()
    {
        return getModem().getNode();
    }

    @Nonnull
    public abstract Map<String, IPeripheral> getPeripherals();

    public boolean exclude( String name )
    {
        return false;
    }
}
