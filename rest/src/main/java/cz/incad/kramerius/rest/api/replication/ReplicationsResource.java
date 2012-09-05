/*
 * Copyright (C) 2012 Pavel Stastny
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.kramerius.rest.api.replication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import cz.incad.kramerius.FedoraAccess;
import cz.incad.kramerius.SolrAccess;
import cz.incad.kramerius.document.model.DCConent;
import cz.incad.kramerius.document.model.utils.DCContentUtils;
import cz.incad.kramerius.document.model.utils.DescriptionUtils;
import cz.incad.kramerius.service.ReplicateException;
import cz.incad.kramerius.service.ReplicationService;
import cz.incad.kramerius.service.ResourceBundleService;
import cz.incad.kramerius.utils.ApplicationURL;
import cz.incad.kramerius.utils.DCUtils;

/**
 * API endpoint for replications
 * @author pavels
 */
@Path("/replication/{pid}")
public class ReplicationsResource {

    @Inject
    ReplicationService replicationService;

    @Inject
    ResourceBundleService resourceBundleService;
    
    @Inject
    Provider<Locale> localesProvider;
    
    @Inject
    @Named("securedFedoraAccess")
    FedoraAccess fedoraAccess;
    
    @Inject
    SolrAccess solrAccess;
    

    @Inject
    Provider<HttpServletRequest> requestProvider;
    
    /**
     * Returns DC content
     * @param pid PID of object
     * @return DC content
     * @throws ReplicateException Cannot get description
     */
    @GET
    @Path("description")
    @Produces(MediaType.APPLICATION_JSON+ ";charset=utf-8")
    public StreamingOutput getExportedDescription(@PathParam("pid") String pid) throws ReplicateException {
        try {
            Map<String, List<DCConent>> dcs = DCContentUtils.getDCS(fedoraAccess, solrAccess, Arrays.asList(pid));
            List<DCConent> list = dcs.get(pid);
            DCConent dcConent = DCConent.collectFirstWin(list);
            String appURL = ApplicationURL.applicationURL(this.requestProvider.get());
            if (!appURL.endsWith("/")) appURL += "/";
            return new DescriptionStreamOutput(dcConent,appURL+"handle/"+pid);
        } catch (IOException e) {
            throw new ReplicateException(e);
        }
    }

    
    /**
     * Prepare all pids for replication
     * @param pid Requested object
     * @return collection of pids needs to be replicated
     * @throws ReplicateException Cannot prepare list
     */
    @GET
    @Path("prepare")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamingOutput prepareExport(@PathParam("pid") String pid) throws ReplicateException {
        // raw generate to request writer
        List<String> pidList = replicationService.prepareExport(pid);
        // cannot use JSON object -> too big data
        return new PIDListStreamOutput(pidList);
    }

    /**
     * Returns exported FOXML enveloped in JSON object
     * @param pid PID of object
     * @return FOXML as JSON
     * @throws ReplicateException Cannot export JSON
     * @throws UnsupportedEncodingException 
     */
    @GET
    @Path("exportedFOXML")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamingOutput getExportedFOXML(@PathParam("pid")String pid) throws ReplicateException, UnsupportedEncodingException {
        // musi se vejit do pameti
        byte[] bytes = replicationService.getExportedFOXML(pid);
        return new FOXMLStreamOutput(bytes);
    }
}
