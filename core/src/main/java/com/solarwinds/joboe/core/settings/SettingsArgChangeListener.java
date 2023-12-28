package com.solarwinds.joboe.core.settings;

import lombok.Getter;

/**
 * Listens to change of {@link SettingsArg} from {@link Settings}
 * 
 * This only gets notified when the value has been changed. 
 * 
 * Take note that changing from null value to non-null and vice versa are considered as change too
 * 
 * @author pluk
 *
 * @param <T>
 */
public abstract class SettingsArgChangeListener<T> {
    @Getter
    private final SettingsArg<T> type;
    private T lastValue;
    
    public SettingsArgChangeListener(SettingsArg<T> type) {
        this.type = type;
    }

    public final void onValue(T value) {
        boolean changed;

        if (lastValue != null) {
            changed = !type.areValuesEqual(lastValue, value);
        } else {
            changed = (value != null);
        }
                
        if (changed) {
            lastValue = value;
            onChange(value);
        }
    }
    
    protected abstract void onChange(T newValue);
}
