package me.earth.headlessmc.runtime;

import me.earth.headlessmc.api.config.Property;
import me.earth.headlessmc.api.config.HmcProperties;

import static me.earth.headlessmc.api.config.PropertyTypes.bool;
import static me.earth.headlessmc.api.config.PropertyTypes.number;

public interface RuntimeProperties extends HmcProperties {
    Property<Long> VM_SIZE = number("hmc.vm_size");
    Property<Boolean> ENABLE_REFLECTION = bool("hmc.enable.reflection");

}
