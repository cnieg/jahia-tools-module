package fr.cnieg.jahia.tools.filter;

import javax.annotation.Priority;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.utils.WebUtils;

@Priority(Priorities.AUTHENTICATION)
public class AuthenticationRequestFilter implements ContainerRequestFilter {

    private static final String REQUIRED_PERMISSON = "adminTemplates";

    private static final String REQUIRED_ROLE = "toolManager";

    @Context
    HttpServletRequest httpServletRequest;

    private Subject getAuthenticatedSubject() {
        try {
            return WebUtils.getAuthenticatedSubject(httpServletRequest);
        } catch (final AuthenticationException e) {
            throw new NotAuthorizedException(e.getMessage(), HttpServletRequest.BASIC_AUTH);
        }
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        JahiaUser user = JCRSessionFactory.getInstance().getCurrentUser();
        String username = JahiaUserManagerService.GUEST_USERNAME;
        if (JahiaUserManagerService.isGuest(user)) {
            Subject subject = getAuthenticatedSubject();
            if (subject != null && subject.hasRole(REQUIRED_ROLE)) {
                // user has the required role: allow access
                return;
            }
        } else {
            try {
                JCRSessionWrapper currentUserSession = JCRSessionFactory.getInstance().getCurrentUserSession();
                final JahiaUser jahiaUser = currentUserSession.getUser();
                username = jahiaUser.getUserKey();
                if (currentUserSession.getRootNode().hasPermission(REQUIRED_PERMISSON)) {
                    requestContext.setSecurityContext(new SecurityContext() {

                        @Override
                        public String getAuthenticationScheme() {
                            return httpServletRequest.getScheme();
                        }

                        @Override
                        public Principal getUserPrincipal() {
                            return jahiaUser;
                        }

                        @Override
                        public boolean isSecure() {
                            return httpServletRequest.isSecure();
                        }

                        @Override
                        public boolean isUserInRole(String role) {
                            return httpServletRequest.isUserInRole(role);
                        }
                    });

                    return;
                }
            } catch (final RepositoryException e) {
                requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(String.format("an error occured %s (see server log for more detail)", e.getMessage() != null ? e.getMessage() : e))
                    .build());
            }
        }

        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
            .entity(String.format("User %s is not allowed to access resource %s", username, httpServletRequest.getRequestURI())).build());
    }
}
