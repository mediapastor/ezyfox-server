package com.tvd12.ezyfoxserver.context;

import com.tvd12.ezyfox.constant.EzyConstant;
import com.tvd12.ezyfox.util.EzyProperties;
import com.tvd12.ezyfoxserver.event.EzyEvent;

public interface EzyContext extends EzyProperties {

	<T> T get(Class<T> clazz);
	
	<T> T cmd(Class<T> clazz);
	
	void fireEvent(EzyConstant type, EzyEvent event);
	
	void handleException(Thread thread, Throwable throwable);
	
}
