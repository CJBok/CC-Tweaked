package dan200.computercraft.api.network.wired;

import javax.annotation.Nonnull;

public interface IWiredNetwork
{
    boolean connect( IWiredNode left, IWiredNode right );

    boolean disconnect( IWiredNode left, IWiredNode right );

    boolean remove( IWiredNode node );
}
