package dan200.computercraft.shared.wired;

import com.google.common.collect.Sets;
import dan200.computercraft.api.network.Packet;
import dan200.computercraft.api.network.wired.IWiredNetwork;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class WiredNetwork implements IWiredNetwork
{
    final ReadWriteLock lock = new ReentrantReadWriteLock();
    private HashSet<WiredNode> nodes;
    private HashMap<String, IPeripheral> peripherals = new HashMap<>();

    public WiredNetwork( WiredNode node )
    {
        nodes = new HashSet<>( 1 );
        nodes.add( node );
    }

    private WiredNetwork( HashSet<WiredNode> nodes )
    {
        this.nodes = nodes;
    }

    @Override
    public boolean connect( @Nonnull IWiredNode nodeU, @Nonnull IWiredNode nodeV )
    {
        WiredNode wiredU = checkNode( nodeU );
        WiredNode wiredV = checkNode( nodeV );
        if( nodeU == nodeV ) throw new IllegalArgumentException( "Cannot add a connection to oneself." );

        lock.writeLock().lock();
        try
        {
            if( nodes == null ) throw new IllegalStateException( "Cannot add a connection to an empty network." );

            boolean hasU = wiredU.network == this;
            boolean hasV = wiredV.network == this;
            if( !hasU && !hasV ) throw new IllegalArgumentException( "Neither node is in the network." );

            // We're going to assimilate a node. Copy across all edges and vertices.
            if( !hasU || !hasV )
            {
                WiredNetwork other = hasU ? wiredV.network : wiredU.network;
                other.lock.writeLock().lock();
                try
                {
                    // Cache several properties for iterating over later
                    Map<String, IPeripheral> otherPeripherals = other.peripherals;
                    Map<String, IPeripheral> thisPeripherals = otherPeripherals.isEmpty() ? peripherals : new HashMap<>( peripherals );

                    Collection<WiredNode> thisNodes = otherPeripherals.isEmpty() ? nodes : new ArrayList<>( this.nodes );
                    Collection<WiredNode> otherNodes = other.nodes;

                    // Move all nodes across into this network, destroying the original nodes.
                    nodes.addAll( otherNodes );
                    for( WiredNode node : otherNodes ) node.network = this;
                    other.nodes = null;

                    // Move all peripherals across, 
                    other.peripherals = null;
                    peripherals.putAll( otherPeripherals );

                    if( !thisPeripherals.isEmpty() )
                    {
                        NetworkChange.added( thisPeripherals ).broadcast( otherNodes );
                    }

                    if( !otherPeripherals.isEmpty() )
                    {
                        NetworkChange.added( otherPeripherals ).broadcast( thisNodes );
                    }
                }
                finally
                {
                    other.lock.writeLock().unlock();
                }
            }

            if( wiredU.neighbours.add( wiredV ) )
            {
                wiredV.neighbours.add( wiredU );
                return true;
            }
            return false;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean disconnect( @Nonnull IWiredNode nodeU, @Nonnull IWiredNode nodeV )
    {
        WiredNode wiredU = checkNode( nodeU );
        WiredNode wiredV = checkNode( nodeV );
        if( nodeU == nodeV ) throw new IllegalArgumentException( "Cannot remove a connection to oneself." );

        lock.writeLock().lock();
        try
        {
            boolean hasU = wiredU.network == this;
            boolean hasV = wiredV.network == this;
            if( !hasU || !hasV ) throw new IllegalArgumentException( "One node is not in the network." );

            // If there was no connection to remove then split.
            if( !wiredU.neighbours.remove( wiredV ) ) return false;
            wiredV.neighbours.remove( wiredU );

            // Determine if there is still some connection from u to v.
            // Note this is an inlining of reachableNodes which short-circuits
            // if all nodes are reachable.
            Queue<WiredNode> enqueued = new ArrayDeque<>();
            HashSet<WiredNode> reachableU = new HashSet<>();

            reachableU.add( wiredU );
            enqueued.add( wiredU );

            while( !enqueued.isEmpty() )
            {
                WiredNode node = enqueued.remove();
                for( WiredNode neighbour : node.neighbours )
                {
                    // If we can reach wiredV from wiredU then abort. 
                    if( neighbour == wiredV ) return true;

                    // Otherwise attempt to enqueue this neighbour as well.
                    if( reachableU.add( neighbour ) ) enqueued.add( neighbour );
                }
            }

            // Create a new network with all U-reachable nodes/edges and remove them
            // from the existing graph.
            WiredNetwork networkU = new WiredNetwork( reachableU );
            networkU.lock.writeLock().lock();
            try
            {
                // Remove nodes from this network
                nodes.removeAll( reachableU );

                // Set network and transfer peripherals
                for( WiredNode node : reachableU )
                {
                    node.network = networkU;
                    networkU.peripherals.putAll( node.peripherals );
                    peripherals.keySet().removeAll( node.peripherals.keySet() );
                }

                // Broadcast changes
                if( peripherals.size() != 0 ) NetworkChange.removed( peripherals ).broadcast( networkU.nodes );
                if( networkU.peripherals.size() != 0 )
                {
                    NetworkChange.removed( networkU.peripherals ).broadcast( nodes );
                }

                return true;
            }
            finally
            {
                networkU.lock.writeLock().unlock();
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove( @Nonnull IWiredNode node )
    {
        WiredNode wired = checkNode( node );

        lock.writeLock().lock();
        try
        {
            // If we're the empty graph then just abort: nodes must have _some_ network.
            if( nodes == null ) return false;
            if( nodes.size() <= 1 ) return false;
            if( wired.network != this ) return false;

            HashSet<WiredNode> neighbours = wired.neighbours;

            // Remove this node and move into a separate network.
            nodes.remove( wired );
            for( WiredNode neighbour : neighbours ) neighbour.neighbours.remove( wired );

            WiredNetwork wiredNetwork = new WiredNetwork( wired );

            // If we're a leaf node in the graph (only one neighbour) then we don't need to 
            // check for network splitting
            if( neighbours.size() == 1 )
            {
                // Broadcast our simple peripheral changes
                splitSingleNetwork( wired, wiredNetwork );
                return true;
            }

            HashSet<WiredNode> reachable = reachableNodes( neighbours.iterator().next() );

            // If all nodes are reachable then exit.
            if( reachable.size() == nodes.size() )
            {
                // Broadcast our simple peripheral changes
                splitSingleNetwork( wired, wiredNetwork );
                return true;
            }

            // A split may cause 2..neighbours.size() separate networks, so we 
            // iterate through our neighbour list, generating child networks.
            neighbours.removeAll( reachable );
            ArrayList<WiredNetwork> maximals = new ArrayList<>( neighbours.size() + 1 );
            maximals.add( wiredNetwork );
            maximals.add( new WiredNetwork( reachable ) );

            while( neighbours.size() > 0 )
            {
                reachable = reachableNodes( neighbours.iterator().next() );
                neighbours.removeAll( reachable );
                maximals.add( new WiredNetwork( reachable ) );
            }

            for( WiredNetwork network : maximals ) network.lock.writeLock().lock();
            try
            {
                // Ensure every network is finalised
                for( WiredNetwork network : maximals )
                {
                    for( WiredNode child : network.nodes )
                    {
                        child.network = network;
                        network.peripherals.putAll( child.peripherals );
                    }
                }

                // Then broadcast network changes once all nodes are finalised
                for( WiredNetwork network : maximals )
                {
                    NetworkChange.changeOf( peripherals, network.peripherals ).broadcast( network.nodes );
                }
            }
            finally
            {
                for( WiredNetwork network : maximals ) network.lock.writeLock().unlock();
            }

            nodes.clear();
            peripherals.clear();

            return true;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void invalidate( @Nonnull IWiredNode node )
    {
        WiredNode wired = checkNode( node );

        lock.writeLock().lock();
        try
        {
            if( wired.network != this ) throw new IllegalStateException( "Node is not on this network" );

            Map<String, IPeripheral> oldPeripherals = wired.peripherals;
            Map<String, IPeripheral> newPeripherals = wired.element.getPeripherals();
            NetworkChange change = NetworkChange.changeOf( oldPeripherals, newPeripherals );
            if( change.isEmpty() ) return;

            wired.peripherals = newPeripherals;

            // Detach the old peripherals then remove them.
            for( IPeripheral peripheral : change.peripheralsRemoved().values() )
            {
                peripheral.detach( node );
            }
            peripherals.keySet().removeAll( change.peripheralsRemoved().keySet() );

            // Add the new peripherals and attach them
            peripherals.putAll( change.peripheralsAdded() );
            for( IPeripheral peripheral : change.peripheralsAdded().values() )
            {
                peripheral.attach( node );
            }

            change.broadcast( nodes );
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    void transmitPacket( WiredNode start, Packet packet, double range, boolean interdimensional )
    {
        Map<WiredNode, TransmitPoint> points = new HashMap<>();
        TreeSet<TransmitPoint> transmitTo = new TreeSet<>();

        {
            TransmitPoint startEntry = new TransmitPoint( start, 0, false );
            points.put( start, startEntry );
            transmitTo.add( startEntry );
        }

        {
            TransmitPoint point;
            while( (point = transmitTo.pollFirst()) != null )
            {
                World world = point.node.element.getWorld();
                Vec3d position = point.node.element.getPosition();
                for( WiredNode neighbour : point.node.neighbours )
                {
                    TransmitPoint neighbourPoint = points.get( neighbour );

                    boolean newInterdimensional;
                    double newDistance;
                    if( world != neighbour.element.getWorld() )
                    {
                        newInterdimensional = true;
                        newDistance = Double.POSITIVE_INFINITY;
                    }
                    else
                    {
                        newInterdimensional = false;
                        newDistance = point.distance + position.distanceTo( neighbour.element.getPosition() );
                    }

                    if( neighbourPoint == null )
                    {
                        neighbourPoint = new TransmitPoint( neighbour, newDistance, newInterdimensional );
                        points.put( neighbour, neighbourPoint );
                        transmitTo.add( neighbourPoint );
                    }
                    else if( newDistance < neighbourPoint.distance )
                    {
                        transmitTo.remove( neighbourPoint );
                        neighbourPoint.distance = newDistance;
                        neighbourPoint.interdimensional = newInterdimensional;
                        transmitTo.add( neighbourPoint );
                    }
                }
            }
        }

        for( TransmitPoint point : points.values() )
        {
            point.node.tryTransmit( packet, point.distance, point.interdimensional, range, interdimensional );
        }
    }

    public Set<WiredNode> getNodes()
    {
        lock.readLock().lock();
        try
        {
            return Sets.newHashSet( nodes );
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    private void splitSingleNetwork( WiredNode wired, WiredNetwork wiredNetwork )
    {
        wiredNetwork.lock.writeLock().lock();
        try
        {
            wired.network = wiredNetwork;
            wiredNetwork.peripherals.putAll( wired.peripherals );
            peripherals.keySet().removeAll( wired.peripherals.keySet() );
            if( peripherals.size() != 0 ) NetworkChange.removed( peripherals ).broadcast( wired );
            if( wired.peripherals.size() != 0 ) NetworkChange.removed( wired.peripherals ).broadcast( nodes );
        }
        finally
        {
            wiredNetwork.lock.writeLock().unlock();
        }
    }

    private static class TransmitPoint implements Comparable<TransmitPoint>
    {
        final WiredNode node;
        double distance;
        boolean interdimensional;

        TransmitPoint( WiredNode node, double distance, boolean interdimensional )
        {
            this.node = node;
            this.distance = distance;
            this.interdimensional = interdimensional;
        }

        @Override
        public int compareTo( @Nonnull TransmitPoint o )
        {
            return Double.compare( distance, o.distance );
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

    private static HashSet<WiredNode> reachableNodes( WiredNode start )
    {
        Queue<WiredNode> enqueued = new ArrayDeque<>();
        HashSet<WiredNode> reachable = new HashSet<>();

        reachable.add( start );
        enqueued.add( start );

        WiredNode node;
        while( (node = enqueued.poll()) != null )
        {
            for( WiredNode neighbour : node.neighbours )
            {
                // Otherwise attempt to enqueue this neighbour as well.
                if( reachable.add( neighbour ) ) enqueued.add( neighbour );
            }
        }

        return reachable;
    }
}