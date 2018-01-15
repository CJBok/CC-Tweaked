package dan200.computercraft.shared.wired;

import com.google.common.collect.Sets;
import dan200.computercraft.api.network.wired.AbstractWiredElement;
import dan200.computercraft.api.network.wired.IWiredNetwork;
import dan200.computercraft.api.network.wired.IWiredNode;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class NetworkTest
{
    @Test
    public void testConnect()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            bE = new NetworkElement( null, null, "b" ),
            cE = new NetworkElement( null, null, "c" );

        IWiredNode
            aN = WiredNode.create( aE ),
            bN = WiredNode.create( bE ),
            cN = WiredNode.create( cE );

        assertNotEquals( "A's and B's network must be different", aN.getNetwork(), bN.getNetwork() );
        assertNotEquals( "A's and C's network must be different", aN.getNetwork(), cN.getNetwork() );
        assertNotEquals( "B's and C's network must be different", bN.getNetwork(), cN.getNetwork() );

        assertTrue( "Must be able to add connection", aN.getNetwork().connect( aN, bN ) );
        assertFalse( "Cannot add connection twice", aN.getNetwork().connect( aN, bN ) );

        assertEquals( "A's and B's network must be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's network should contain A and B", Sets.newHashSet( aN, bN ), nodes( aN.getNetwork() ) );

        aN.getNetwork().connect( aN, cN );

        assertEquals( "A's and B's network must be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and C's network must be equal", aN.getNetwork(), cN.getNetwork() );
        assertEquals( "A's network should contain A, B and C", Sets.newHashSet( aN, bN, cN ), nodes( aN.getNetwork() ) );

        assertEquals( "A's neighbour set should be B, C", Sets.newHashSet( bN, cN ), neighbours( aN.getNetwork(), aN ) );
        assertEquals( "B's neighbour set should be A", Sets.newHashSet( aN ), neighbours( aN.getNetwork(), bN ) );
        assertEquals( "C's neighbour set should be A", Sets.newHashSet( aN ), neighbours( aN.getNetwork(), cN ) );
    }

    @Test
    public void testDisconnectNoChange()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            bE = new NetworkElement( null, null, "b" ),
            cE = new NetworkElement( null, null, "c" );

        IWiredNode
            aN = WiredNode.create( aE ),
            bN = WiredNode.create( bE ),
            cN = WiredNode.create( cE );

        aN.getNetwork().connect( aN, bN );
        aN.getNetwork().connect( aN, cN );
        aN.getNetwork().connect( bN, cN );

        aN.getNetwork().disconnect( aN, bN );

        assertEquals( "A's and B's network must be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and C's network must be equal", aN.getNetwork(), cN.getNetwork() );
        assertEquals( "A's network should contain A, B and C", Sets.newHashSet( aN, bN, cN ), nodes( aN.getNetwork() ) );
    }

    @Test
    public void testDisconnectLeaf()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            bE = new NetworkElement( null, null, "b" ),
            cE = new NetworkElement( null, null, "c" );

        IWiredNode
            aN = WiredNode.create( aE ),
            bN = WiredNode.create( bE ),
            cN = WiredNode.create( cE );

        aN.getNetwork().connect( aN, bN );
        aN.getNetwork().connect( aN, cN );

        aN.getNetwork().disconnect( aN, bN );

        assertNotEquals( "A's and B's network must not be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and C's network must be equal", aN.getNetwork(), cN.getNetwork() );
        assertEquals( "A's network should contain A and C", Sets.newHashSet( aN, cN ), nodes( aN.getNetwork() ) );
        assertEquals( "B's network should contain B", Sets.newHashSet( bN ), nodes( bN.getNetwork() ) );
    }

    @Test
    public void testDisconnectSplit()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            aaE = new NetworkElement( null, null, "a_" ),
            bE = new NetworkElement( null, null, "b" ),
            bbE = new NetworkElement( null, null, "b_" );

        IWiredNode
            aN = WiredNode.create( aE ),
            aaN = WiredNode.create( aaE ),
            bN = WiredNode.create( bE ),
            bbN = WiredNode.create( bbE );

        aN.getNetwork().connect( aN, aaN );
        bN.getNetwork().connect( bN, bbN );

        aN.getNetwork().connect( aN, bN );

        aN.getNetwork().disconnect( aN, bN );

        assertNotEquals( "A's and B's network must not be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and A_'s network must be equal", aN.getNetwork(), aaN.getNetwork() );
        assertEquals( "B's and B_'s network must be equal", bN.getNetwork(), bbN.getNetwork() );

        assertEquals( "A's network should contain A and A_", Sets.newHashSet( aN, aaN ), nodes( aN.getNetwork() ) );
        assertEquals( "B's network should contain B and B_", Sets.newHashSet( bN, bbN ), nodes( bN.getNetwork() ) );
    }

    @Test
    public void testRemoveLeaf()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            bE = new NetworkElement( null, null, "b" ),
            cE = new NetworkElement( null, null, "c" );

        IWiredNode
            aN = WiredNode.create( aE ),
            bN = WiredNode.create( bE ),
            cN = WiredNode.create( cE );

        aN.getNetwork().connect( aN, bN );
        aN.getNetwork().connect( aN, cN );

        assertTrue( "Must be able to remove node", aN.getNetwork().remove( bN ) );
        assertFalse( "Cannot remove a second time", aN.getNetwork().remove( bN ) );

        assertNotEquals( "A's and B's network must not be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and C's network must be equal", aN.getNetwork(), cN.getNetwork() );

        assertEquals( "A's network should contain A and C", Sets.newHashSet( aN, cN ), nodes( aN.getNetwork() ) );
        assertEquals( "B's network should contain B", Sets.newHashSet( bN ), nodes( bN.getNetwork() ) );
    }

    @Test
    public void testRemoveSplit()
    {
        NetworkElement
            aE = new NetworkElement( null, null, "a" ),
            aaE = new NetworkElement( null, null, "a_" ),
            bE = new NetworkElement( null, null, "b" ),
            bbE = new NetworkElement( null, null, "b_" ),
            cE = new NetworkElement( null, null, "c" );

        IWiredNode
            aN = WiredNode.create( aE ),
            aaN = WiredNode.create( aaE ),
            bN = WiredNode.create( bE ),
            bbN = WiredNode.create( bbE ),
            cN = WiredNode.create( cE );

        aN.getNetwork().connect( aN, aaN );
        bN.getNetwork().connect( bN, bbN );

        cN.getNetwork().connect( aN, cN );
        cN.getNetwork().connect( bN, cN );

        cN.getNetwork().remove( cN );

        assertNotEquals( "A's and B's network must not be equal", aN.getNetwork(), bN.getNetwork() );
        assertEquals( "A's and A_'s network must be equal", aN.getNetwork(), aaN.getNetwork() );
        assertEquals( "B's and B_'s network must be equal", bN.getNetwork(), bbN.getNetwork() );

        assertEquals( "A's network should contain A and A_", Sets.newHashSet( aN, aaN ), nodes( aN.getNetwork() ) );
        assertEquals( "B's network should contain B and B_", Sets.newHashSet( bN, bbN ), nodes( bN.getNetwork() ) );
    }

    @Test
    public void testLarge() throws InterruptedException
    {
        final int BRUTE_SIZE = 64;
        final int TOGGLE_CONNECTION_TIMES = 5;
        final int TOGGLE_NODE_TIMES = 5;

        Grid<IWiredNode> grid = new Grid<>( BRUTE_SIZE );
        grid.map( ( existing, pos ) -> WiredNode.create( new NetworkElement( null, null, "n_" + pos ) ) );

        // Test connecting
        {
            long start = System.nanoTime();

            grid.forEach( ( existing, pos ) -> {
                for( EnumFacing facing : EnumFacing.VALUES )
                {
                    BlockPos offset = pos.offset( facing );
                    if( (offset.getX() > BRUTE_SIZE / 2) == (pos.getX() > BRUTE_SIZE / 2) )
                    {
                        IWiredNode other = grid.get( offset );
                        if( other != null ) existing.getNetwork().connect( existing, other );
                    }
                }
            } );

            long end = System.nanoTime();

            System.out.printf( "Connecting %s nodes took %s seconds\n", BRUTE_SIZE, (end - start) * 1e-9 );
        }

        // Test toggling
        {
            IWiredNode left = grid.get( new BlockPos( BRUTE_SIZE / 2, 0, 0 ) );
            IWiredNode right = grid.get( new BlockPos( BRUTE_SIZE / 2 + 1, 0, 0 ) );
            assertNotEquals( left.getNetwork(), right.getNetwork() );

            long start = System.nanoTime();
            for( int i = 0; i < TOGGLE_CONNECTION_TIMES; i++ )
            {
                left.getNetwork().connect( left, right );
                left.getNetwork().disconnect( left, right );
            }

            long end = System.nanoTime();

            System.out.printf( "Toggling connection %s times took %s seconds\n", TOGGLE_CONNECTION_TIMES, (end - start) * 1e-9 );
        }

        {
            IWiredNode left = grid.get( new BlockPos( BRUTE_SIZE / 2, 0, 0 ) );
            IWiredNode right = grid.get( new BlockPos( BRUTE_SIZE / 2 + 1, 0, 0 ) );
            IWiredNode centre = WiredNode.create( new NetworkElement( null, null, "c" ) );
            assertNotEquals( left.getNetwork(), right.getNetwork() );

            long start = System.nanoTime();
            for( int i = 0; i < TOGGLE_NODE_TIMES; i++ )
            {
                left.getNetwork().connect( left, centre );
                right.getNetwork().connect( right, centre );

                left.getNetwork().remove( centre );
            }

            long end = System.nanoTime();

            System.out.printf( "Toggling %s node took %s seconds\n", TOGGLE_NODE_TIMES, (end - start) * 1e-9 );
        }
    }

    private class NetworkElement extends AbstractWiredElement
    {
        private final World world;
        private final Vec3d position;
        private final String id;

        private NetworkElement( World world, Vec3d position, String id )
        {
            this.world = world;
            this.position = position;
            this.id = id;
        }

        @Nonnull
        @Override
        public World getWorld()
        {
            return world;
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            return position;
        }

        @Nonnull
        @Override
        public String getSenderID()
        {
            return id;
        }

        @Override
        public String toString()
        {
            return "NetworkElement{" + id + "}";
        }
    }

    private class Grid<T>
    {
        private final int size;
        private final T[] box;

        @SuppressWarnings("unchecked")
        public Grid( int size )
        {
            this.size = size;
            this.box = (T[]) new Object[size * size * size];
        }

        public void set( BlockPos pos, T elem )
        {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();

            if( x >= 0 && x < size && y >= 0 && y < size && z >= 0 && z < size )
            {
                box[x * size * size + y * size + z] = elem;
            }
            else
            {
                throw new IndexOutOfBoundsException( pos.toString() );
            }
        }

        public T get( BlockPos pos )
        {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();

            return x >= 0 && x < size && y >= 0 && y < size && z >= 0 && z < size
                ? box[x * size * size + y * size + z]
                : null;
        }

        public void forEach( BiConsumer<T, BlockPos> transform )
        {
            for( int x = 0; x < size; x++ )
            {
                for( int y = 0; y < size; y++ )
                {
                    for( int z = 0; z < size; z++ )
                    {
                        transform.accept( box[x * size * size + y * size + z], new BlockPos( x, y, z ) );
                    }
                }
            }
        }

        public void map( BiFunction<T, BlockPos, T> transform )
        {
            for( int x = 0; x < size; x++ )
            {
                for( int y = 0; y < size; y++ )
                {
                    for( int z = 0; z < size; z++ )
                    {
                        box[x * size * size + y * size + z] = transform.apply( box[x * size * size + y * size + z], new BlockPos( x, y, z ) );
                    }
                }
            }
        }
    }

    private static Set<WiredNode> nodes( IWiredNetwork network )
    {
        return ((WiredNetwork) network).graph.nodes();
    }

    private static Set<WiredNode> neighbours( IWiredNetwork network, IWiredNode node )
    {
        return ((WiredNetwork) network).graph.successors( node );
    }
}
