package dan200.computercraft.shared.peripheral.modem;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.network.wired.IWiredSender;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static dan200.computercraft.core.apis.ArgumentHelper.getString;

public class WiredModemPeripheral extends ModemPeripheral implements IWiredSender
{
    private final WiredModemElement modem;

    private final Map<String, RemotePeripheralWrapper> peripheralWrappers = new HashMap<>();

    public WiredModemPeripheral( WiredModemElement modem )
    {
        this.modem = modem;
    }

    //region IPacketSender implementation
    @Override
    public boolean isInterdimensional()
    {
        return false;
    }

    @Override
    public double getRange()
    {
        return 256.0;
    }

    @Override
    protected IPacketNetwork getNetwork()
    {
        return modem.getNode();
    }

    @Nonnull
    @Override
    public World getWorld()
    {
        return modem.getWorld();
    }

    @Nonnull
    @Override
    public Vec3d getPosition()
    {
        return modem.getPosition();
    }
    //endregion

    //region IPeripheral
    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        String[] methods = super.getMethodNames();
        String[] newMethods = new String[methods.length + 5];
        System.arraycopy( methods, 0, newMethods, 0, methods.length );
        newMethods[methods.length] = "getNamesRemote";
        newMethods[methods.length + 1] = "isPresentRemote";
        newMethods[methods.length + 2] = "getTypeRemote";
        newMethods[methods.length + 3] = "getMethodsRemote";
        newMethods[methods.length + 4] = "callRemote";
        return newMethods;
    }

    @Override
    public Object[] callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException, InterruptedException
    {
        String[] methods = super.getMethodNames();
        switch( method - methods.length )
        {
            case 0:
            {
                // getNamesRemote
                synchronized( peripheralWrappers )
                {
                    int idx = 1;
                    Map<Object, Object> table = new HashMap<>();
                    for( String name : peripheralWrappers.keySet() )
                    {
                        table.put( idx++, name );
                    }
                    return new Object[]{ table };
                }
            }
            case 1:
            {
                // isPresentRemote
                String type = getTypeRemote( getString( arguments, 0 ) );
                return new Object[]{ type != null };
            }
            case 2:
            {
                // getTypeRemote
                String type = getTypeRemote( getString( arguments, 0 ) );
                if( type != null )
                {
                    return new Object[]{ type };
                }
                return null;
            }
            case 3:
            {
                // getMethodsRemote
                String[] methodNames = getMethodNamesRemote( getString( arguments, 0 ) );
                if( methodNames != null )
                {
                    Map<Object, Object> table = new HashMap<>();
                    for( int i = 0; i < methodNames.length; ++i )
                    {
                        table.put( i + 1, methodNames[i] );
                    }
                    return new Object[]{ table };
                }
                return null;
            }
            case 4:
            {
                // callRemote
                String remoteName = getString( arguments, 0 );
                String methodName = getString( arguments, 1 );
                Object[] methodArgs = new Object[arguments.length - 2];
                System.arraycopy( arguments, 2, methodArgs, 0, arguments.length - 2 );
                return callMethodRemote( remoteName, context, methodName, methodArgs );
            }
            default:
            {
                // The regular modem methods
                return super.callMethod( computer, context, method, arguments );
            }
        }
    }

    @Override
    public void attach( @Nonnull IComputerAccess computer )
    {
        super.attach( computer );
        synchronized( modem.getRemotePeripherals() )
        {
            synchronized( peripheralWrappers )
            {
                for( Map.Entry<String, IPeripheral> entry : modem.getRemotePeripherals().entrySet() )
                {
                    attachPeripheralImpl( entry.getKey(), entry.getValue() );
                }
            }
        }
    }

    @Override
    public synchronized void detach( @Nonnull IComputerAccess computer )
    {
        synchronized( peripheralWrappers )
        {
            for( RemotePeripheralWrapper wrapper : peripheralWrappers.values() )
            {
                wrapper.detach();
            }
            peripheralWrappers.clear();
        }
        super.detach( computer );
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        if( other instanceof WiredModemPeripheral )
        {
            WiredModemPeripheral otherModem = (WiredModemPeripheral) other;
            return otherModem.modem == modem;
        }
        return false;
    }
    //endregion

    @Nonnull
    @Override
    public IWiredNode getNode()
    {
        return modem.getNode();
    }

    public void attachPeripheral( String name, IPeripheral peripheral )
    {
        if( getComputer() == null ) return;

        synchronized( peripheralWrappers )
        {
            attachPeripheralImpl( name, peripheral );
        }
    }

    public void detachPeripheral( String name )
    {
        synchronized( peripheralWrappers )
        {
            RemotePeripheralWrapper wrapper = peripheralWrappers.get( name );
            if( wrapper != null )
            {
                peripheralWrappers.remove( name );
                wrapper.detach();
            }
        }
    }

    private void attachPeripheralImpl( String periphName, IPeripheral peripheral )
    {
        if( !peripheralWrappers.containsKey( periphName ) )
        {
            RemotePeripheralWrapper wrapper = new RemotePeripheralWrapper( peripheral, getComputer(), periphName );
            peripheralWrappers.put( periphName, wrapper );
            wrapper.attach();
        }
    }

    private String getTypeRemote( String remoteName )
    {
        synchronized( peripheralWrappers )
        {
            RemotePeripheralWrapper wrapper = peripheralWrappers.get( remoteName );
            if( wrapper != null )
            {
                return wrapper.getType();
            }
        }
        return null;
    }

    private String[] getMethodNamesRemote( String remoteName )
    {
        synchronized( peripheralWrappers )
        {
            RemotePeripheralWrapper wrapper = peripheralWrappers.get( remoteName );
            if( wrapper != null )
            {
                return wrapper.getMethodNames();
            }
        }
        return null;
    }

    private Object[] callMethodRemote( String remoteName, ILuaContext context, String method, Object[] arguments ) throws LuaException, InterruptedException
    {
        RemotePeripheralWrapper wrapper;
        synchronized( peripheralWrappers )
        {
            wrapper = peripheralWrappers.get( remoteName );
        }
        if( wrapper != null )
        {
            return wrapper.callMethod( context, method, arguments );
        }
        throw new LuaException( "No peripheral: " + remoteName );
    }

    private static class RemotePeripheralWrapper implements IComputerAccess
    {
        private IPeripheral m_peripheral;
        private IComputerAccess m_computer;
        private String m_name;

        private String m_type;
        private String[] m_methods;
        private Map<String, Integer> m_methodMap;

        public RemotePeripheralWrapper( IPeripheral peripheral, IComputerAccess computer, String name )
        {
            m_peripheral = peripheral;
            m_computer = computer;
            m_name = name;

            m_type = peripheral.getType();
            m_methods = peripheral.getMethodNames();
            assert (m_type != null);
            assert (m_methods != null);

            m_methodMap = new HashMap<>();
            for( int i = 0; i < m_methods.length; ++i )
            {
                if( m_methods[i] != null )
                {
                    m_methodMap.put( m_methods[i], i );
                }
            }
        }

        public void attach()
        {
            m_peripheral.attach( this );
            m_computer.queueEvent( "peripheral", new Object[]{ getAttachmentName() } );
        }

        public void detach()
        {
            m_peripheral.detach( this );
            m_computer.queueEvent( "peripheral_detach", new Object[]{ getAttachmentName() } );
        }

        public String getType()
        {
            return m_type;
        }

        public String[] getMethodNames()
        {
            return m_methods;
        }

        public Object[] callMethod( ILuaContext context, String methodName, Object[] arguments ) throws LuaException, InterruptedException
        {
            if( m_methodMap.containsKey( methodName ) )
            {
                int method = m_methodMap.get( methodName );
                return m_peripheral.callMethod( this, context, method, arguments );
            }
            throw new LuaException( "No such method " + methodName );
        }

        // IComputerAccess implementation

        @Override
        public String mount( @Nonnull String desiredLocation, @Nonnull IMount mount )
        {
            return m_computer.mount( desiredLocation, mount, m_name );
        }

        @Override
        public String mount( @Nonnull String desiredLocation, @Nonnull IMount mount, @Nonnull String driveName )
        {
            return m_computer.mount( desiredLocation, mount, driveName );
        }

        @Override
        public String mountWritable( @Nonnull String desiredLocation, @Nonnull IWritableMount mount )
        {
            return m_computer.mountWritable( desiredLocation, mount, m_name );
        }

        @Override
        public String mountWritable( @Nonnull String desiredLocation, @Nonnull IWritableMount mount, @Nonnull String driveName )
        {
            return m_computer.mountWritable( desiredLocation, mount, driveName );
        }

        @Override
        public void unmount( String location )
        {
            m_computer.unmount( location );
        }

        @Override
        public int getID()
        {
            return m_computer.getID();
        }

        @Override
        public void queueEvent( @Nonnull String event, Object[] arguments )
        {
            m_computer.queueEvent( event, arguments );
        }

        @Nonnull
        @Override
        public String getAttachmentName()
        {
            return m_name;
        }
    }
}
