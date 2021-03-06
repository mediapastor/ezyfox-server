package com.tvd12.ezyfoxserver.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@JsonIgnoreProperties({"zonesByIds", "zonesByNames", "zoneNames", "zoneIds"})
@JsonPropertyOrder({"size", "zones"})
public class EzySimpleZonesSetting implements EzyZonesSetting {

	protected final List<EzyZoneSetting> zones = new ArrayList<>();
	protected final Map<Integer, EzySimpleZoneSetting> zonesByIds = new ConcurrentHashMap<>();
	protected final Map<String, EzySimpleZoneSetting> zonesByNames = new ConcurrentHashMap<>();

	public void setItem(EzySimpleZoneSetting item) {
		zones.add(item);
		zonesByIds.put(item.getId(), item);
		zonesByNames.put(item.getName(), item);
	}
	
	@Override
	public Set<String> getZoneNames() {
		return zonesByNames.keySet();
	}
	
	@Override
	public Set<Integer> getZoneIds() {
		return zonesByIds.keySet();
	}
	
	@Override
	public EzySimpleZoneSetting getZoneByName(String name) {
		if(zonesByNames.containsKey(name))
			return zonesByNames.get(name);
		throw new IllegalArgumentException("has no zone with name: " + name);
	}
	
	@Override
	public EzySimpleZoneSetting getZoneById(Integer id) {
		if(zonesByIds.containsKey(id))
			return zonesByIds.get(id);
		throw new IllegalArgumentException("has no zone with id: " + id);
	}
	
	@Override
	public int getSize() {
		return zones.size();
	}
	
}
