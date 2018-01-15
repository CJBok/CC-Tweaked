package dan200.computercraft.shared.wired;

import com.google.common.graph.*;
import dan200.computercraft.api.network.wired.IWiredNetwork;
import dan200.computercraft.api.network.wired.IWiredNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WiredNetwork implements IWiredNetwork
{
    private final Object graphLock = new Object();
    MutableGraph<WiredNode> graph;

    public WiredNetwork( WiredNode node )
    {
        this.graph = GraphBuilder.<WiredNode>undirected()
            .nodeOrder( ElementOrder.unordered() )
            .expectedNodeCount( 1 )
            .build();

        graph.addNode( node );
    }

    private WiredNetwork( MutableGraph<WiredNode> graph )
    {
        this.graph = graph;
    }

    @Override
    public boolean connect( IWiredNode nodeU, IWiredNode nodeV )
    {
        WiredNode wiredU = checkNode( nodeU );
        WiredNode wiredV = checkNode( nodeV );
        if( nodeU == nodeV ) throw new IllegalArgumentException( "Cannot add a connection to oneself." );
        if( graph == null ) throw new IllegalArgumentException( "Cannot add a connection in an empty network." );

        synchronized( graphLock )
        {
            boolean hasU = graph.nodes().contains( wiredU );
            boolean hasV = graph.nodes().contains( wiredV );
            if( !hasU && !hasV ) throw new IllegalArgumentException( "Neither node is in the network." );

            // We're going to assimilate a node. Copy across all edges and vertices.
            if( !hasU || !hasV )
            {
                WiredNetwork other = hasU ? wiredV.network : wiredU.network;
                synchronized( other.graphLock )
                {
                    for( WiredNode node : other.graph.nodes() )
                    {
                        graph.addNode( node );
                        node.network = this;
                    }

                    for( EndpointPair<WiredNode> edges : other.graph.edges() )
                    {
                        graph.putEdge( edges.nodeU(), edges.nodeV() );
                    }

                    // There's no way to reinstate this network, so let the GC destroy it.
                    other.graph = null;

                    // TODO: Update all node peripherals
                }
            }

            return graph.putEdge( (WiredNode) nodeU, (WiredNode) nodeV );
        }
    }

    @Override
    public boolean disconnect( IWiredNode nodeU, IWiredNode nodeV )
    {
        WiredNode wiredU = checkNode( nodeU );
        WiredNode wiredV = checkNode( nodeV );
        if( nodeU == nodeV ) throw new IllegalArgumentException( "Cannot remove a connection to oneself." );
        if( graph == null ) throw new IllegalArgumentException( "Cannot remove a connection from an empty network." );

        synchronized( graphLock )
        {
            boolean hasU = graph.nodes().contains( wiredU );
            boolean hasV = graph.nodes().contains( wiredV );
            if( !hasU || !hasV ) throw new IllegalArgumentException( "One node is not in the network." );

            // If there was no connection to remove then split.
            if( !graph.removeEdge( wiredU, wiredV ) ) return false;

            // Determine if there is still some connection from u to v.
            Set<WiredNode> reachableU = Graphs.reachableNodes( graph, wiredU );
            if( reachableU.contains( wiredV ) ) return true;

            // Create a new subgraph with all U-reachable nodes/edges and remove them
            // from the existing graph.
            WiredNetwork networkU = new WiredNetwork( Graphs.inducedSubgraph( graph, reachableU ) );
            for( WiredNode node : reachableU )
            {
                graph.removeNode( node );
                node.network = networkU;
            }

            // TODO: Update all node peripherals

            return true;
        }
    }

    @Override
    public boolean remove( IWiredNode node )
    {
        WiredNode wired = checkNode( node );
        if( graph == null ) return false;

        synchronized( graphLock )
        {
            // If we're the empty graph then just abort: nodes must have _some_ network.
            if( graph.nodes().size() <= 1 ) return false;

            if( !graph.nodes().contains( wired ) ) return false;

            Set<WiredNode> neighbours = graph.adjacentNodes( wired );

            // Remove this node and move into a separate network.
            graph.removeNode( wired );
            wired.network = new WiredNetwork( wired );

            // TODO: Update all node peripherals

            // If we're a leaf node in the graph (only one neighbour) then we don't need to 
            // check for network splitting
            if( neighbours.size() == 1 )
            {
                return true;
            }

            Set<WiredNode> reachable = Graphs.reachableNodes( graph, neighbours.iterator().next() );

            // If all nodes are reachable then exit.
            if( reachable.size() == graph.nodes().size() )
            {
                return true;
            }

            neighbours = new HashSet<>( neighbours ); // Make neighbours mutable

            // A split may cause 2..neighbours.size() separate networks, so we 
            // iterate through our neighbour list, generating child networks.
            neighbours.removeAll( reachable );
            List<WiredNetwork> maximals = new ArrayList<>( neighbours.size() );
            maximals.add( new WiredNetwork( Graphs.inducedSubgraph( graph, reachable ) ) );

            while( neighbours.size() > 0 )
            {
                reachable = Graphs.reachableNodes( graph, neighbours.iterator().next() );
                neighbours.removeAll( reachable );
                maximals.add( new WiredNetwork( Graphs.inducedSubgraph( graph, reachable ) ) );
            }

            for( WiredNetwork network : maximals )
            {
                for( WiredNode child : network.graph.nodes() )
                {
                    child.network = network;
                }
            }

            graph = null;
            // TODO: Update all node peripherals

            return true;
        }
    }

    private static WiredNode checkNode( IWiredNode node )
    {
        if( node instanceof WiredNode )
        {
            return (WiredNode) node;
        }
        else
        {
            throw new IllegalArgumentException( "Unknown implementation of IWiredNode: " + node );
        }
    }
}
