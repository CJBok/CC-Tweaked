/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.common.BlockCable;
import dan200.computercraft.shared.util.IDAssigner;
import dan200.computercraft.shared.util.PeripheralUtil;
import dan200.computercraft.shared.wired.IWiredElementTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

public class TileWiredModemFull extends TileWiredBase implements IWiredElementTile
{
    private boolean m_peripheralAccessAllowed = false;
    private int[] m_attachedPeripheralIDs = new int[6];
    private boolean m_destroyed = false;
    private boolean m_connectionsFormed = false;
    
    public TileWiredModemFull( )
    {
        Arrays.fill( m_attachedPeripheralIDs, -1 );
    }

    @Override
    public void destroy()
    {
        if( !m_destroyed )
        {
            m_destroyed = true;
            getNode().remove();
        }
        super.destroy();
    }

    @Override
    public EnumFacing getDirection()
    {
        return EnumFacing.NORTH;
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
    }

    @Override
    public void onNeighbourChange()
    {
        if( !world.isRemote && m_peripheralAccessAllowed )
        {
            // Fetch the old set of IDs and the current peripheral
            int[] ids = Arrays.copyOf( m_attachedPeripheralIDs, m_attachedPeripheralIDs.length );
            Map<String, IPeripheral> updated = getPeripherals();

            if( updated.isEmpty() )
            {
                // If there are no peripherals then disable access and update the display state.
                m_peripheralAccessAllowed = false;
                updateAnim();
            }

            if( updated.isEmpty() || !Arrays.equals( ids, m_attachedPeripheralIDs ) )
            {
                // If there are no peripherals or the IDs have changed then update the node. 
                getNode().invalidate();
            }
        }
    }

    @Nonnull
    @Override
    public AxisAlignedBB getBounds()
    {
        return BlockCable.FULL_BLOCK_AABB;
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        if( !getWorld().isRemote )
        {
            // On server, we interacted if a peripheral was found
            Set<String> oldPeriphName = getPeripherals().keySet();
            togglePeripheralAccess();
            Set<String> periphName = getPeripherals().keySet();

            if( !Objects.equal( periphName, oldPeriphName ) )
            {
                if( !oldPeriphName.isEmpty() )
                {
                    List<String> names = new ArrayList<>( oldPeriphName );
                    names.sort( Comparator.naturalOrder() );

                    // TODO: Move localisation to a separate string.
                    player.sendMessage(
                        new TextComponentTranslation( "gui.computercraft:wired_modem.peripheral_disconnected", String.join( ", ", names ) )
                    );
                }
                if( !periphName.isEmpty() )
                {
                    List<String> names = new ArrayList<>( periphName );
                    names.sort( Comparator.naturalOrder() );
                    player.sendMessage(
                        new TextComponentTranslation( "gui.computercraft:wired_modem.peripheral_connected", String.join( ", ", names ) )
                    );
                }
            }

            return true;
        }
        else
        {
            // On client, we can't know this, so we assume so to be safe
            // The server will correct us if we're wrong
            return true;
        }
    }

    @Override
    public void readFromNBT( NBTTagCompound tag )
    {
        // Read properties
        super.readFromNBT( tag );
        m_peripheralAccessAllowed = tag.getBoolean( "peripheralAccess" );
        for( int i = 0; i < m_attachedPeripheralIDs.length; i++ )
        {
            String key = "peripheralID_" + i;
            if( tag.hasKey( key, Constants.NBT.TAG_ANY_NUMERIC ) )
            {
                m_attachedPeripheralIDs[i] = tag.getInteger( key );
            }
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound tag )
    {
        // Write properties
        tag = super.writeToNBT( tag );
        tag.setBoolean( "peripheralAccess", m_peripheralAccessAllowed );
        for( int i = 0; i < m_attachedPeripheralIDs.length; i++ )
        {
            tag.setInteger( "peripheralID_" + i, m_attachedPeripheralIDs[i] );
        }
        return tag;
    }

    @Override
    protected void updateAnim()
    {
        int anim = 0;
        if( m_modem.isActive() )
        {
            anim += 1;
        }
        if( m_peripheralAccessAllowed )
        {
            anim += 2;
        }
        setAnim( anim );
    }

    @Override
    public void update()
    {
        super.update();
        if( !getWorld().isRemote )
        {
            if( !m_connectionsFormed )
            {
                networkChanged();
                m_connectionsFormed = true;
            }
        }
    }

    public void networkChanged()
    {
        if( getWorld().isRemote ) return;

        World world = getWorld();
        BlockPos current = getPos();
        for( EnumFacing facing : EnumFacing.VALUES )
        {
            IWiredElement element = ComputerCraft.getWiredElementAt( world, current.offset( facing ), facing.getOpposite() );
            if( element == null ) continue;

            // If we can connect to it then do so
            getNode().connectTo( element.getNode() );
        }

        getNode().invalidate();
    }

    // private stuff
    public void togglePeripheralAccess()
    {
        if( !m_peripheralAccessAllowed )
        {
            m_peripheralAccessAllowed = true;
            if( getPeripherals().isEmpty() )
            {
                m_peripheralAccessAllowed = false;
                return;
            }
        }
        else
        {
            m_peripheralAccessAllowed = false;
        }

        updateAnim();
        getNode().invalidate();
    }

    @Override
    @Nonnull
    public Map<String, IPeripheral> getPeripherals()
    {
        if( !m_peripheralAccessAllowed ) return Collections.emptyMap();

        Map<String, IPeripheral> peripherals = new HashMap<>( 6 );
        for( EnumFacing facing : EnumFacing.VALUES )
        {
            BlockPos neighbour = getPos().offset( facing );
            IPeripheral peripheral = PeripheralUtil.getPeripheral( getWorld(), neighbour, facing.getOpposite() );
            if( peripheral != null && !(peripheral instanceof WiredModemPeripheral) )
            {
                String type = peripheral.getType();
                int id = m_attachedPeripheralIDs[facing.ordinal()];
                if( id < 0 )
                {
                    id = m_attachedPeripheralIDs[facing.ordinal()] = IDAssigner.getNextIDFromFile( new File(
                        ComputerCraft.getWorldDir( getWorld() ),
                        "computer/lastid_" + type + ".txt"
                    ) );
                }

                peripherals.put( type + "_" + id, peripheral );
            }
        }

        return peripherals;
    }

    @Override
    public boolean canRenderBreaking()
    {
        return true;
    }

    // IWiredElement tile

    @Override
    public IWiredElement getWiredElement( EnumFacing side )
    {
        return getModem();
    }

    // IPeripheralTile

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return m_modem;
    }
}
