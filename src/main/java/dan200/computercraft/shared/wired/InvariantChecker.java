package dan200.computercraft.shared.wired;

import dan200.computercraft.ComputerCraft;

public class InvariantChecker
{
    public static void checkNode( WiredNode node )
    {
        WiredNetwork network = node.network;
        if( network == null )
        {
            ComputerCraft.log.error( "Node's network is null", new Exception() );
        }
        else if( network.nodes == null || !network.nodes.contains( node ) )
        {
            ComputerCraft.log.error( "Node's network does not contain node", new Exception() );
        }

        for( WiredNode neighbour : node.neighbours )
        {
            if( !neighbour.neighbours.contains( node ) )
            {
                ComputerCraft.log.error( "Neighbour is missing node", new Exception() );
            }
        }
    }

    public static void checkNetwork( WiredNetwork network )
    {
        for( WiredNode node : network.nodes )
        {
            checkNode( node );
        }
    }
}
