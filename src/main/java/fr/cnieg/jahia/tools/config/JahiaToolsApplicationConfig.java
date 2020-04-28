package fr.cnieg.jahia.tools.config;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import fr.cnieg.jahia.tools.filter.AuthenticationRequestFilter;
import fr.cnieg.jahia.tools.resource.SiteResource;

public class JahiaToolsApplicationConfig extends ResourceConfig {

    public JahiaToolsApplicationConfig() {
        super(MultiPartFeature.class, JacksonJaxbJsonProvider.class, AuthenticationRequestFilter.class, SiteResource.class);
    }
}
