package org.betonquest.betonquest.quest.event.journal;

import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.quest.event.Event;
import org.betonquest.betonquest.api.quest.event.EventFactory;
import org.betonquest.betonquest.api.quest.event.StaticEvent;
import org.betonquest.betonquest.database.Connector;
import org.betonquest.betonquest.database.Saver;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.quest.event.DatabaseSaverStaticEvent;
import org.betonquest.betonquest.quest.event.DoNothingStaticEvent;
import org.betonquest.betonquest.quest.event.InfoNotificationSender;
import org.betonquest.betonquest.quest.event.NoNotificationSender;
import org.betonquest.betonquest.quest.event.NotificationSender;
import org.betonquest.betonquest.quest.event.OnlinePlayerGroupStaticEventAdapter;
import org.betonquest.betonquest.quest.event.SequentialStaticEvent;
import org.betonquest.betonquest.utils.Utils;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Factory to create journal events from {@link Instruction}s.
 */
public class JournalEventFactory implements EventFactory {
    /**
     * The saver to inject into database-using events.
     */
    private final Saver saver;

    /**
     * The server to use for fetching server-data.
     */
    private final Server server;

    /**
     * Create the journal event factory.
     *
     * @param saver  database saver to use
     * @param server server to refer to
     */
    public JournalEventFactory(final Saver saver, final Server server) {
        this.saver = saver;
        this.server = server;
    }

    @Override
    public Event parseEvent(final Instruction instruction) throws InstructionParseException {
        final String action = instruction.next();
        return switch (action.toLowerCase(Locale.ROOT)) {
            case "update" -> createJournalUpdateEvent();
            case "add" -> createJournalAddEvent(instruction);
            case "delete" -> createJournalDeleteEvent(instruction);
            default -> throw new InstructionParseException("Unknown journal action: " + action);
        };
    }

    @Override
    public StaticEvent parseStaticEvent(final Instruction instruction) throws InstructionParseException {
        final String action = instruction.next();
        return switch (action.toLowerCase(Locale.ROOT)) {
            case "update", "add" -> new DoNothingStaticEvent();
            case "delete" -> createStaticJournalDeleteEvent(instruction);
            default -> throw new InstructionParseException("Unknown journal action: " + action);
        };
    }

    @NotNull
    private JournalEvent createJournalDeleteEvent(final Instruction instruction) throws InstructionParseException {
        final String entryName = Utils.addPackage(instruction.getPackage(), instruction.next());
        final JournalChanger journalChanger = new RemoveEntryJournalChanger(entryName);
        final NotificationSender notificationSender = new NoNotificationSender();
        return new JournalEvent(journalChanger, notificationSender);
    }

    @NotNull
    private JournalEvent createJournalAddEvent(final Instruction instruction) throws InstructionParseException {
        final String entryName = Utils.addPackage(instruction.getPackage(), instruction.next());
        final JournalChanger journalChanger = new AddEntryJournalChanger(entryName);
        final NotificationSender notificationSender = new InfoNotificationSender("new_journal_entry", instruction.getPackage(), instruction.getID().getFullID());
        return new JournalEvent(journalChanger, notificationSender);
    }

    @NotNull
    private JournalEvent createJournalUpdateEvent() {
        final JournalChanger journalChanger = new NoActionJournalChanger();
        final NotificationSender notificationSender = new NoNotificationSender();
        return new JournalEvent(journalChanger, notificationSender);
    }

    @NotNull
    private StaticEvent createStaticJournalDeleteEvent(final Instruction instruction) throws InstructionParseException {
        final JournalEvent journalDeleteEvent = createJournalDeleteEvent(instruction.copy());
        final String entryName = Utils.addPackage(instruction.getPackage(), instruction.next());
        return new SequentialStaticEvent(
                new OnlinePlayerGroupStaticEventAdapter(server::getOnlinePlayers, journalDeleteEvent),
                new DatabaseSaverStaticEvent(saver, () -> new Saver.Record(Connector.UpdateType.REMOVE_ALL_ENTRIES, entryName))
        );
    }
}
