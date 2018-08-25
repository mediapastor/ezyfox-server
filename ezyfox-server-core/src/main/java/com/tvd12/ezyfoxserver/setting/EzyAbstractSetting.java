package com.tvd12.ezyfoxserver.setting;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tvd12.ezyfox.util.EzyEquals;
import com.tvd12.ezyfox.util.EzyHashCodes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.NONE)
public abstract class EzyAbstractSetting 
        implements EzyBaseSetting, EzyZoneIdAware, EzyHomePathAware {

	protected final int id = newId();
	
	@XmlElement(name = "name")
	protected String name;
	
	@XmlElement(name = "folder")
    protected String folder;
	
	protected int zoneId;
	
	@XmlElement(name = "entry-loader")
	protected String entryLoader;
	
	@XmlElement(name = "thread-pool-size")
	protected int threadPoolSize = 0;
	
	@XmlElement(name = "config-file")
	protected String configFile = "config.properties";
	
	@JsonIgnore
	protected String homePath = "";
	
	@JsonIgnore
	protected String location = "";
	
	protected int newId() {
	    return getIdCounter().incrementAndGet();
	}
	
	@Override
	public String getFolder() {
	    return StringUtils.isEmpty(folder) ? name : folder;
	}
	
	@Override
    public String getConfigFile() {
        return Paths.get(getLocation(), configFile).toString();
    }
	
	protected abstract AtomicInteger getIdCounter();
	
	@Override
    public boolean equals(Object obj) {
        return new EzyEquals<EzyAbstractSetting>()
                .function(t -> t.id)
                .isEquals(this, obj);
    }
	
	@Override
	public int hashCode() {
	    return new EzyHashCodes().append(id).toHashCode();
	}
	
}
