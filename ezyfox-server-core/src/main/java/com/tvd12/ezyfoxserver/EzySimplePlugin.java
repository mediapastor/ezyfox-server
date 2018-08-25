package com.tvd12.ezyfoxserver;

import com.tvd12.ezyfox.util.EzyDestroyable;
import com.tvd12.ezyfox.util.EzyEquals;
import com.tvd12.ezyfox.util.EzyHashCodes;
import com.tvd12.ezyfoxserver.setting.EzyPluginSetting;
import com.tvd12.ezyfoxserver.wrapper.EzyEventControllers;
import com.tvd12.ezyfoxserver.wrapper.impl.EzyEventPluginControllersImpl;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EzySimplePlugin 
        extends EzyChildComponent 
        implements EzyPlugin, EzyDestroyable {

    protected EzyPluginSetting setting;
    
    @Override
    protected EzyEventControllers newEventControllers() {
        return EzyEventPluginControllersImpl.builder().build();
    }
    
    @Override
    public boolean equals(Object obj) {
        return new EzyEquals<EzySimplePlugin>()
                .function(t -> t.setting)
                .isEquals(this, obj);
    }
    
    @Override
    public int hashCode() {
        return new EzyHashCodes()
                .append(setting)
                .hashCode();
    }
    
}
