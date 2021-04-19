/*
 * Copyright (C) 2010 Pavel Stastny
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
package cz.incad.Kramerius.security.rightscommands.post;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;

import cz.incad.Kramerius.security.RightsServlet;
import cz.incad.Kramerius.security.ServletCommand;
import cz.incad.Kramerius.security.rightscommands.ServletRightsCommand;
import cz.incad.kramerius.ObjectPidsPath;
import cz.incad.kramerius.security.Right;
import cz.incad.kramerius.security.SecuredActions;
import cz.incad.kramerius.security.SecurityException;
import cz.incad.kramerius.security.impl.RightImpl;

public class Edit extends ServletRightsCommand {

    static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Edit.class.getName());
    
    @Override
    public void doCommand() {
        try {
            HttpServletRequest req = this.requestProvider.get();
            //Right right = RightsServlet.createRightFromPost(req, rightsManager, userManager, criteriumWrapperFactory);
            Map values = new HashMap();
            Enumeration parameterNames = req.getParameterNames();

            Delete.parametersToJson(req, values, parameterNames);

            List affectedObjects = (List) values.get("affectedObjects");
            for (int i = 0; i < affectedObjects.size(); i++) {
                String pid = affectedObjects.get(i).toString();
                editRight((Map) values.get("data"), pid);
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        } catch(Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        }
    }

    private void editRight(Map data, String pid) throws IOException, SQLException {
        RightImpl right = right(data, pid);
        ObjectPidsPath[] paths = this.solrAccess.getPath(pid);
        boolean hasRight = false;
        for (int i = 0; i < paths.length; i++) {
            if (this.actionAllowed.isActionAllowed(SecuredActions.ADMINISTRATE.getFormalName(), pid, null, paths[i]).flag()) {
                hasRight = true;
                break;
            } else {
                throw new SecurityException(new SecurityException.SecurityExceptionInfo(SecuredActions.ADMINISTRATE,pid));
            }
        } 
        // root object
        if (paths.length  == 0) {
            if (this.actionAllowed.isActionAllowed(SecuredActions.ADMINISTRATE.getFormalName(), pid, null ,new ObjectPidsPath(pid)).flag()) {
                hasRight = true;
            } else {
                throw new SecurityException(new SecurityException.SecurityExceptionInfo(SecuredActions.ADMINISTRATE,pid));
            }
        }
        
        if (hasRight) {
            rightsManager.updateRight(right);
        }
        
    }
    
}
