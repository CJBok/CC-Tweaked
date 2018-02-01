package dan200.computercraft.api.network.wired;

import javax.annotation.Nonnull;

public interface IWiredNetwork
{
    boolean connect( @Nonnull IWiredNode left, @Nonnull IWiredNode right );

    boolean disconnect( @Nonnull IWiredNode left, @Nonnull IWiredNode right );

    boolean remove( @Nonnull IWiredNode node );

    void invalidate( @Nonnull IWiredNode node );
}
