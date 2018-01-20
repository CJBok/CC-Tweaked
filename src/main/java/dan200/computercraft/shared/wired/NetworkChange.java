package dan200.computercraft.shared.wired;

import dan200.computercraft.api.network.wired.INetworkChange;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NetworkChange implements INetworkChange
{
    private final Map<String, IPeripheral> removed;
    private final Map<String, IPeripheral> added;

    private NetworkChange( Map<String, IPeripheral> removed, Map<String, IPeripheral> added )
    {
        this.removed = removed;
        this.added = added;
    }

    public static NetworkChange changed( Map<String, IPeripheral> removed, Map<String, IPeripheral> added )
    {
        return new NetworkChange( Collections.unmodifiableMap( removed ), Collections.unmodifiableMap( added ) );
    }

    public static NetworkChange added( Map<String, IPeripheral> added )
    {
        return new NetworkChange( Collections.emptyMap(), Collections.unmodifiableMap( added ) );
    }

    public static NetworkChange removed( Map<String, IPeripheral> removed )
    {
        return new NetworkChange( Collections.unmodifiableMap( removed ), Collections.emptyMap() );
    }

    public static NetworkChange changeOf( Map<String, IPeripheral> oldPeripherals, Map<String, IPeripheral> newPeripherals )
    {
        Map<String, IPeripheral> added = new HashMap<>( newPeripherals );
        Map<String, IPeripheral> removed = new HashMap<>();

        for( Map.Entry<String, IPeripheral> entry : oldPeripherals.entrySet() )
        {
            String oldKey = entry.getKey();
            IPeripheral oldValue = entry.getValue();
            if( newPeripherals.containsKey( oldKey ) )
            {
                IPeripheral rightValue = added.get( oldKey );
                if( oldValue.equals( rightValue ) )
                {
                    added.remove( oldKey );
                }
                else
                {
                    removed.put( oldKey, oldValue );
                }
            }
            else
            {
                removed.put( oldKey, oldValue );
            }
        }

        return changed( removed, added );
    }

    @Override
    public Map<String, IPeripheral> peripheralsAdded()
    {
        return added;
    }

    @Nonnull
    @Override
    public Map<String, IPeripheral> peripheralsRemoved()
    {
        return removed;
    }

    void broadcast( Iterable<WiredNode> nodes )
    {
        for( WiredNode node : nodes ) node.element.networkChanged( this );
    }

    void broadcast( WiredNode node )
    {
        node.element.networkChanged( this );
    }
}
