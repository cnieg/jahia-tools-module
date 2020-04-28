package fr.cnieg.jahia.tools.resource;

import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

import javax.jcr.RepositoryException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jahia.exceptions.JahiaException;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.importexport.ImportExportBaseService;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import fr.cnieg.jahia.tools.resource.bean.OperationResult;
import fr.cnieg.jahia.tools.util.ZipUtil;

@Slf4j
@Path("/api/jahia-tools/sites")
@Produces({ MediaType.APPLICATION_JSON })
public class SiteResource {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importSite(@FormDataParam("file") final FormDataBodyPart file, @FormDataParam("site") final String site)
        throws RepositoryException {
        if (file == null || site == null) {
            throw new ClientErrorException("File and site are required", Response.Status.BAD_REQUEST);
        }

        JCRTemplate.getInstance().doExecuteWithSystemSession(jcrSessionWrapper -> {
            ImportExportBaseService importService = ImportExportBaseService.getInstance();
            log.info("Starting site import from jahia tools api");
            FormDataContentDisposition fileDisposition = file.getFormDataContentDisposition();
            try (InputStream bundleInputStream = file.getValueAs(InputStream.class)) {
                Resource siteImportResource = getUploadedFileAsResource(bundleInputStream, fileDisposition.getFileName());
                log.info("Import from file {}", siteImportResource.getFilename());

                java.nio.file.Path zipPath = siteImportResource.getFile().toPath();
                ZipUtil.unzip(zipPath, StandardCharsets.UTF_8);

                String fileBaseName = FilenameUtils.getBaseName(zipPath.getFileName().toString());
                java.nio.file.Path destFolderPath = Paths.get(zipPath.getParent().toString(), fileBaseName);

                java.nio.file.Path siteZip = destFolderPath.resolve(site + ".zip");
                importService.importSiteZip(new FileSystemResource(siteZip.toFile()), jcrSessionWrapper);
            } catch (final IOException | RepositoryException e) {
                throw new InternalServerErrorException("Error while importing site", e);
            }

            return null;
        });

        log.info("Import successful");
        OperationResult result = OperationResult.builder().message("Import successful").build();
        return Response.ok(result).build();
    }

    @DELETE
    @Path("/{site}")
    public Response deleteSite(@PathParam("site") final String site) throws JahiaException {
        JahiaSitesService jahiaSiteService = JahiaSitesService.getInstance();
        JahiaSite jahiaSite = jahiaSiteService.getSiteByKey(site);
        if (jahiaSite == null) {
            OperationResult result = OperationResult.builder().message("Unable to find site with key " + site).build();
            return Response.serverError().entity(result).build();
        }

        log.info("Start deleting site {}", site);
        jahiaSiteService.removeSite(jahiaSite);

        log.info("Delete successful");
        OperationResult result = OperationResult.builder().message("Delete successful").build();
        return Response.ok(result).build();
    }

    private static Resource getUploadedFileAsResource(final InputStream inputStream, final String filename) throws IOException {
        File tempFile = File.createTempFile(getBaseName(filename) + '-', '.' + getExtension(filename), getTempDirectory());
        FileUtils.copyInputStreamToFile(inputStream, tempFile);
        return new FileSystemResource(tempFile);
    }
}
