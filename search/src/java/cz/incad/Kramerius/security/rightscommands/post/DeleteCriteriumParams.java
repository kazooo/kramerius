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
package cz.incad.Kramerius.security.rightscommands.post;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.incad.Kramerius.security.rightscommands.ServletRightsCommand;
import cz.incad.kramerius.ObjectPidsPath;
import cz.incad.kramerius.security.SecuredActions;
import cz.incad.kramerius.security.SpecialObjects;

public class DeleteCriteriumParams extends ServletRightsCommand {

    static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(DeleteCriteriumParams.class.getName());

    @Override
    public void doCommand() {
        try {

            HttpServletRequest req = this.requestProvider.get();
            Map values = new HashMap();
            Enumeration parameterNames = req.getParameterNames();

            Delete.parametersToJson(req, values, parameterNames);

            Object paramsToDelete = values.get("deletedparams");

            if (this.actionAllowed.isActionAllowed(SecuredActions.CRITERIA_RIGHTS_MANAGE.getFormalName(), SpecialObjects.REPOSITORY.getPid(), null, new ObjectPidsPath(SpecialObjects.REPOSITORY.getPid())).flag()) {
                rightsManager.deleteRightCriteriumParams(Integer.parseInt(paramsToDelete.toString()));
                
            } else {
                this.responseProvider.get().sendError(HttpServletResponse.SC_FORBIDDEN);
            }

        } catch (SQLException e) {
            try {
                this.responseProvider.get().sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException e1) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }


}
