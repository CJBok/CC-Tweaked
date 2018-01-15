package dan200.computercraft.api.network.wired;

public abstract class AbstractWiredElement implements IWiredElement
{
    private IWiredNode node;

    @Override
    public final IWiredNode getNode()
    {
        return node;
    }

    @Override
    public void setNode( IWiredNode node )
    {
        if( node == null ) throw new IllegalStateException( "node cannot be null" );
        if( this.node != null ) throw new IllegalStateException( "This element already has a node." );

        this.node = node;
    }
}
