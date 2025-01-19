package org.betonquest.betonquest.quest.event.language;

import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.event.Event;
import org.betonquest.betonquest.data.PlayerDataStorage;
import org.betonquest.betonquest.exception.QuestException;

/**
 * Changes player's language.
 */
public class LanguageEvent implements Event {

    /**
     * The language to set.
     */
    private final String language;

    /**
     * Storage for player data.
     */
    private final PlayerDataStorage dataStorage;

    /**
     * Create the language event.
     *
     * @param language    the language to set
     * @param dataStorage the storage providing player data
     */
    public LanguageEvent(final String language, final PlayerDataStorage dataStorage) {
        this.language = language;
        this.dataStorage = dataStorage;
    }

    @Override
    public void execute(final Profile profile) throws QuestException {
        dataStorage.getOffline(profile).setLanguage(language);
    }
}
