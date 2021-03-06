package com.tvd12.ezyfoxserver.controller;

import static com.tvd12.ezyfoxserver.context.EzyServerContexts.containsUser;
import static com.tvd12.ezyfoxserver.context.EzyServerContexts.getSessionManager;
import static com.tvd12.ezyfoxserver.context.EzyServerContexts.getStatistics;
import static com.tvd12.ezyfoxserver.context.EzyZoneContexts.getZoneSetting;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

import com.tvd12.ezyfox.builder.EzyArrayBuilder;
import com.tvd12.ezyfox.entity.EzyArray;
import com.tvd12.ezyfox.entity.EzyData;
import com.tvd12.ezyfox.util.EzyEntityBuilders;
import com.tvd12.ezyfoxserver.EzyZone;
import com.tvd12.ezyfoxserver.constant.EzyDisconnectReason;
import com.tvd12.ezyfoxserver.constant.EzyEventType;
import com.tvd12.ezyfoxserver.constant.EzyLoginError;
import com.tvd12.ezyfoxserver.context.EzyAppContext;
import com.tvd12.ezyfoxserver.context.EzyServerContext;
import com.tvd12.ezyfoxserver.context.EzyZoneContext;
import com.tvd12.ezyfoxserver.entity.EzyAbstractSession;
import com.tvd12.ezyfoxserver.entity.EzySession;
import com.tvd12.ezyfoxserver.entity.EzySimpleUser;
import com.tvd12.ezyfoxserver.entity.EzyUser;
import com.tvd12.ezyfoxserver.event.EzyEvent;
import com.tvd12.ezyfoxserver.event.EzySimpleSessionLoginEvent;
import com.tvd12.ezyfoxserver.event.EzySimpleUserAddedEvent;
import com.tvd12.ezyfoxserver.event.EzyUserLoginEvent;
import com.tvd12.ezyfoxserver.exception.EzyLoginErrorException;
import com.tvd12.ezyfoxserver.response.EzyLoginParams;
import com.tvd12.ezyfoxserver.response.EzyLoginResponse;
import com.tvd12.ezyfoxserver.response.EzyResponse;
import com.tvd12.ezyfoxserver.setting.EzyAppSetting;
import com.tvd12.ezyfoxserver.setting.EzyUserManagementSetting;
import com.tvd12.ezyfoxserver.setting.EzyZoneSetting;
import com.tvd12.ezyfoxserver.statistics.EzyUserStatistics;
import com.tvd12.ezyfoxserver.wrapper.EzySessionManager;
import com.tvd12.ezyfoxserver.wrapper.EzyZoneUserManager;

public class EzyLoginProcessor extends EzyEntityBuilders {

    private final EzyUserStatistics userStats;
    private final EzyServerContext serverContext;
    private final EzySessionManager<EzySession> sessionManager;
    
    private static final AtomicLong GUEST_COUNT = new AtomicLong();
    
    public EzyLoginProcessor(EzyServerContext serverContext) {
        this.serverContext = serverContext;
        	this.sessionManager = getSessionManager(serverContext);
        	this.userStats = getStatistics(serverContext).getUserStats();
    }
    
    public void apply(EzyZoneContext zoneContext, EzyUserLoginEvent event) {
        EzyZone zone = zoneContext.getZone();
        EzyZoneSetting zoneSetting = zone.getSetting();
        EzyUserManagementSetting userManagementSetting = zoneSetting.getUserManagement();
        String username = checkUsername(event.getUsername(),
                userManagementSetting.getUserNamePattern(),
                userManagementSetting.isAllowGuestLogin(),
                userManagementSetting.getGuestNamePrefix());
        String password = event.getPassword();
        EzyZoneUserManager userManager = zone.getUserManager();
        EzyUser user = null;
        EzySession session = event.getSession();
        boolean alreadyLoggedIn = false;
        Lock lock = userManager.getLock(username);
        lock.lock();
        try {
            alreadyLoggedIn = userManager.containsUser(username);
            user = alreadyLoggedIn 
                    ? userManager.getUser(username)
                    : newUser(zoneSetting, userManagementSetting, username, password);
            int maxSessionPerUser = userManagementSetting.getMaxSessionPerUser();
            boolean allowChangeSession = userManagementSetting.isAllowChangeSession();
            processUserSessions(user, session, maxSessionPerUser, allowChangeSession);
            addUserToManager(userManager, user, session, alreadyLoggedIn);
        }
        finally {
            lock.unlock();
        }
        fireUserAddedEvent(zoneContext, user, session, event.getData(), alreadyLoggedIn);
        EzyResponse response = newLoginReponse(zoneContext, user, event.getOutput());
        serverContext.send(response, session);
        fireSessionLoginEvent(zoneContext, user, session, alreadyLoggedIn);
    }
    
    protected String checkUsername(
            String username,
            String userNamePattern,
            boolean allowGuestLogin, String guestNamePrefix) {
        if(username != null && username.matches(userNamePattern))
            return username;
        if(allowGuestLogin) {
            long userId = GUEST_COUNT.incrementAndGet();
            String answer = guestNamePrefix + userId;
            return answer;
        }
        throw new EzyLoginErrorException(EzyLoginError.INVALID_USERNAME);
    }
    
    protected void processUserSessions(EzyUser user,
            EzySession session,
            int maxSessionPerUser,
            boolean allowChangeSession) {
        if(maxSessionPerUser <= 0)
            throw new EzyLoginErrorException(EzyLoginError.MAXIMUM_SESSION);
        int sessionCount = user.getSessionCount();
        if(maxSessionPerUser > 1) {
            if(sessionCount >= maxSessionPerUser)
                throw new EzyLoginErrorException(EzyLoginError.MAXIMUM_SESSION);
        }
        session.setLoggedIn(true);
        session.setLoggedInTime(System.currentTimeMillis());
        ((EzyAbstractSession)session).setOwner(user);
        if(sessionCount == 0) {
            user.addSession(session);
        }
        else if(maxSessionPerUser == 1) {
            if(allowChangeSession)
                processChangeSession(user, session);
        }
        else {
            user.addSession(session);
        }
        sessionManager.addLoggedInSession(session);
    }
    
    protected void processChangeSession(EzyUser user, EzySession session) {
        List<EzySession> oldsessions = user.changeSession(session);
        for(EzySession oldsession : oldsessions)
            sessionManager.removeSession(oldsession, EzyDisconnectReason.ANOTHER_SESSION_LOGIN);
    }

    protected void addUserToManager(EzyZoneUserManager userManager,
            EzyUser user, 
            EzySession session, boolean alreadyLoggedIn) {
        if(alreadyLoggedIn) {
            userManager.bind(session, user);
        }
        else {
            userManager.addUser(session, user);
            userStats.addUsers(1);
            userStats.setCurrentUsers(userManager.getUserCount());
        }
    }
    
    protected void fireSessionLoginEvent(
            EzyZoneContext zoneContext, 
            EzyUser user,
            EzySession session, boolean alreadyLoggedIn) {
        if(alreadyLoggedIn) {
            EzyEvent event = newSessionLoginEvent(user, session);
            zoneContext.fireAppEvent(EzyEventType.USER_SESSION_LOGIN, event, user.getName());
        }
    }
    
    protected void fireUserAddedEvent(
            EzyZoneContext zoneContext,
            EzyUser user,
            EzySession session,
            EzyData loginData, boolean alreadyLoggedIn) {
        if(!alreadyLoggedIn) {
            EzyEvent event = newUserAddedEvent(user, session, loginData);
            fireUserAddedEvent0(zoneContext, event);
        }
    }
    
    protected void fireUserAddedEvent0(
            EzyZoneContext zoneContext, EzyEvent event) {
        try {
            zoneContext.firePluginEvent(EzyEventType.USER_ADDED, event);
        }
        catch(Exception e) {
            getLogger().error("user added error", e);
        }
    }
    
    protected EzyUser newUser(
            EzyZoneSetting zoneSetting,
            EzyUserManagementSetting userManagementSetting,
            String newUserName,
            String password) {
        EzySimpleUser user = new EzySimpleUser();
        user.setName(newUserName);
        user.setPassword(password);
        user.setZoneId(zoneSetting.getId());
        user.setMaxIdleTime(userManagementSetting.getUserMaxIdleTime());
        user.setMaxSessions(userManagementSetting.getMaxSessionPerUser());
        return user;
    }
    
    protected EzyEvent newSessionLoginEvent(EzyUser user, EzySession session) {
        return new EzySimpleSessionLoginEvent(user, session);
    }
    
    protected EzyEvent newUserAddedEvent(EzyUser user, 
            EzySession session, EzyData loginData) {
        return new EzySimpleUserAddedEvent(user, session, loginData);
    }
    
    protected EzyResponse newLoginReponse(
            EzyZoneContext zoneContext, 
            EzyUser user, EzyData loginOuputData) {
        EzyZoneSetting zoneSetting = getZoneSetting(zoneContext);
        EzyLoginParams params = new EzyLoginParams();
        EzyArray joinedApps = getJoinedAppsDetails(zoneContext, user.getName());
        params.setData(loginOuputData);
        params.setUserId(user.getId());
        params.setUsername(user.getName());
        params.setJoinedApps(joinedApps);
        params.setZoneId(zoneSetting.getId());
        params.setZoneName(zoneSetting.getName());
        return new EzyLoginResponse(params);
    }
    
    protected EzyArray getJoinedAppsDetails(
            EzyZoneContext zoneContext, String username) {
        EzyArrayBuilder builder = newArrayBuilder();
        Collection<EzyAppContext> appContexts = zoneContext.getAppContexts();
        for(EzyAppContext appCtx : appContexts) {
            if(containsUser(appCtx, username))
                builder.append(newJoinedAppInfo(appCtx));
        }
        EzyArray answer = builder.build();
        return answer;
    }
    
    @SuppressWarnings("unchecked")
    protected EzyArrayBuilder newJoinedAppInfo(EzyAppContext appCtx) {
        EzyAppSetting app = appCtx.getApp().getSetting();
        EzyArrayBuilder builder = newArrayBuilder()
                .append(app.getId(), app.getName());
        return builder;
    }
    
}
