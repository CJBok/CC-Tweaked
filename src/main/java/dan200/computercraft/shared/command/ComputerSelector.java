package dan200.computercraft.shared.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.command.CommandException;

import java.util.*;
import java.util.function.Predicate;

public final class ComputerSelector
{
    private static ServerComputer getComputer( Predicate<ServerComputer> predicate, String kind ) throws CommandException
    {
        // We copy it to prevent concurrent modifications.
        List<ServerComputer> computers = Lists.newArrayList( ComputerCraft.serverComputerRegistry.getComputers() );
        List<ServerComputer> candidates = Lists.newArrayList();
        for( ServerComputer searchComputer : computers )
        {
            if( predicate.test( searchComputer ) ) candidates.add( searchComputer );
        }

        if( candidates.size() == 0 )
        {
            throw new CommandException( "No such computer for " + kind );
        }
        else if( candidates.size() == 1 )
        {
            return candidates.get( 0 );
        }
        else
        {
            StringBuilder builder = new StringBuilder( "Multiple computers with " )
                .append( kind ).append( " (instances " );

            boolean first = true;
            for( ServerComputer computer : candidates )
            {
                if( first )
                {
                    first = false;
                }
                else
                {
                    builder.append( ", " );
                }

                builder.append( computer.getInstanceID() );
            }

            builder.append( ")" );

            throw new CommandException( builder.toString() );
        }
    }

    public static ServerComputer getComputer( String selector ) throws CommandException
    {
        if( selector.length() > 0 && selector.charAt( 0 ) == '#' )
        {
            selector = selector.substring( 1 );

            int id;
            try
            {
                id = Integer.parseInt( selector );
            }
            catch( NumberFormatException e )
            {
                throw new CommandException( "'" + selector + "' is not a valid number" );
            }

            return getComputer( x -> x.getID() == id, "id " + id );
        }
        else if( selector.length() > 0 && selector.charAt( 0 ) == '@' )
        {
            String label = selector.substring( 1 );
            return getComputer( x -> Objects.equals( label, x.getLabel() ), "label '" + label + "'" );
        }
        else if( selector.length() > 0 && selector.charAt( 0 ) == '~' )
        {
            String familyName = selector.substring( 1 );
            return getComputer( x -> x.getFamily().name().equalsIgnoreCase( familyName ), "family '" + familyName + "'" );
        }
        else
        {
            int instance;
            try
            {
                instance = Integer.parseInt( selector );
            }
            catch( NumberFormatException e )
            {
                throw new CommandException( "'" + selector + "' is not a valid number" );
            }

            ServerComputer computer = ComputerCraft.serverComputerRegistry.get( instance );
            if( computer == null )
            {
                throw new CommandException( "No such computer for instance id " + instance );
            }
            else
            {
                return computer;
            }
        }
    }

    public static List<String> completeComputer( String selector )
    {
        TreeSet<String> options = Sets.newTreeSet();

        // We copy it to prevent concurrent modifications.
        List<ServerComputer> computers = Lists.newArrayList( ComputerCraft.serverComputerRegistry.getComputers() );

        if( selector.length() > 0 && selector.charAt( 0 ) == '#' )
        {
            selector = selector.substring( 1 );

            for( ServerComputer computer : computers )
            {
                String id = Integer.toString( computer.getID() );
                if( id.startsWith( selector ) ) options.add( "#" + id );
            }
        }
        else if( selector.length() > 0 && selector.charAt( 0 ) == '@' )
        {
            String label = selector.substring( 1 );
            for( ServerComputer computer : computers )
            {
                String thisLabel = computer.getLabel();
                if( thisLabel != null && thisLabel.startsWith( label ) ) options.add( "@" + thisLabel );
            }
        }
        else if( selector.length() > 0 && selector.charAt( 0 ) == '~' )
        {
            String familyName = selector.substring( 1 ).toLowerCase( Locale.ENGLISH );
            for( ComputerFamily family : ComputerFamily.values() )
            {
                if( family.name().toLowerCase( Locale.ENGLISH ).startsWith( familyName ) )
                {
                    options.add( "~" + family.name() );
                }
            }
        }
        else
        {
            for( ServerComputer computer : computers )
            {
                String id = Integer.toString( computer.getInstanceID() );
                if( id.startsWith( selector ) ) options.add( id );
            }
        }

        if( options.size() > 100 )
        {
            ArrayList<String> result = Lists.newArrayListWithCapacity( 100 );
            for( String element : options )
            {
                if( result.size() > 100 ) break;
                result.add( element );
            }

            return result;
        }
        else
        {
            return Lists.newArrayList( options );
        }
    }
}
