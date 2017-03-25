package org.dukecon.server.services;

import org.dukecon.model.*;
import org.dukecon.server.adapter.ConferenceDataProvider;
import org.dukecon.services.ConferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.flex.remoting.RemotingDestination;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by christoferdutz on 02.08.16.
 */

@Service("conferenceService")
@RemotingDestination
public class ConferenceServiceImpl implements ConferenceService, ServletContextAware {
    private final Logger log = LoggerFactory.getLogger(ConferenceServiceImpl.class);

    private ServletContext servletContext;

    @Inject
    private List<ConferenceDataProvider> talkProviders;

    private Map<String, Conference> conferences;

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void create(Conference obj) {
        log.debug("create(Conference)");
    }

    @Override
    public Conference read(String id) {
        if(conferences == null) {
            list();
        }
        return conferences.get(id);
    }

    @Override
    public void update(Conference obj) {
        log.debug("update(Conference)");
    }

    @Override
    public void delete(Conference obj) {
        log.debug("delete(Conference)");
    }

    @Override
    public void delete(String id) {
        log.debug("delete(Conference)");
    }

    @Override
    public Collection<Conference> list() {
        Map<String, Conference> conferences = new HashMap<>();
        for(ConferenceDataProvider provider : talkProviders) {
            Conference conference = provider.getConference();
            if(conference == null) {
                continue;
            }

            // When reading things from the input json there are multiple instances of many entities,
            // This code makes sure only one entity instance is used throughout the object graph.
            Map<String, Audience> audienceMap = new HashMap<>();
            Map<String, EventType> eventTypeMap = new HashMap<>();
            Map<String, Language> languageMap = new HashMap<>();
            Map<String, Location> locationMap = new HashMap<>();
            Map<String, Speaker> speakerMap = new HashMap<>();
            Map<String, Track> trackMap = new HashMap<>();
            Map<String, Event> eventMap = new HashMap<>();
            if(conference.getMetaData().getAudiences() != null) {
                for (Audience audience : conference.getMetaData().getAudiences()) {
                    audienceMap.put(audience.getId(), audience);
                }
            }
            if(conference.getMetaData().getEventTypes() != null) {
                for (EventType eventType : conference.getMetaData().getEventTypes()) {
                    eventTypeMap.put(eventType.getId(), eventType);
                }
            }
            if(conference.getMetaData().getLanguages() != null) {
                for (Language language : conference.getMetaData().getLanguages()) {
                    languageMap.put(language.getId(), language);
                }
            }
            conference.getMetaData().setDefaultLanguage(languageMap.get(conference.getMetaData().getDefaultLanguage().getId()));
            if(conference.getMetaData().getLocations() != null) {
                for (Location location : conference.getMetaData().getLocations()) {
                    locationMap.put(location.getId(), location);
                }
            }
            if(conference.getSpeakers() != null) {
                for (Speaker speaker : conference.getSpeakers()) {
                    speakerMap.put(speaker.getId(), speaker);
                }
            }
            if(conference.getMetaData().getTracks() != null) {
                for (Track track : conference.getMetaData().getTracks()) {
                    trackMap.put(track.getId(), track);
                }
            }
            if(conference.getEvents() != null) {
                for (Event event : conference.getEvents()) {
                    if (event.getAudience() != null) {
                        event.setAudience(audienceMap.get(event.getAudience().getId()));
                    }
                    if (event.getTrack() != null) {
                        event.setTrack(trackMap.get(event.getTrack().getId()));
                    }
                    if (event.getLanguage() != null) {
                        event.setLanguage(languageMap.get(event.getLanguage().getId()));
                    }
                    if (event.getLocation() != null) {
                        event.setLocation(locationMap.get(event.getLocation().getId()));
                    }
                    if (event.getType() != null) {
                        event.setType(eventTypeMap.get(event.getType().getId()));
                    }
                    List<Speaker> speakers = event.getSpeakers().stream().map(
                            speaker -> speakerMap.get(speaker.getId())).collect(Collectors.toCollection(LinkedList::new));
                    event.setSpeakers(speakers);
                    eventMap.put(event.getId(), event);
                }
            }
            if(conference.getSpeakers() != null) {
                conference.getSpeakers().stream().filter(speaker -> speaker.getEvents() != null).forEach(speaker -> {
                    List<Event> events = speaker.getEvents().stream().map(
                            event -> eventMap.get((event != null) ? event.getId() : "")).collect(Collectors.toCollection(LinkedList::new));
                    speaker.setEvents(events);
                });
            }
            conference.getMetaData().setId(conference.getId());

            conferences.put(conference.getId(), conference);
        }
        this.conferences = conferences;
        return conferences.values();
    }

    @Override
    public Styles getConferenceStyles(String conferenceId) {
        InputStream is = servletContext.getResourceAsStream(
                "/WEB-INF/conferences/" + conferenceId + ".properties");
        if(is != null) {
            Properties conferenceProperties = new Properties();
            try {
                conferenceProperties.load(is);
                return new Styles(conferenceProperties);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

}
