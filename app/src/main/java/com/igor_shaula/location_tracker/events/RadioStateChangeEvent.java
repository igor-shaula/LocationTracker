package com.igor_shaula.location_tracker.events;

/**
 * Created by igor shaula - to monitor state of GPS and internet availability \
 */
public class RadioStateChangeEvent {

    private final String whatIsChanged; // anyway this filed does not change - it may be final \

    public RadioStateChangeEvent(String whatIsChanged) {
        this.whatIsChanged = whatIsChanged;
    }

    public String getWhatIsChanged() {
        return whatIsChanged;
    }
}