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
package cz.incad.Kramerius.views;

import java.io.IOException;
import java.util.ResourceBundle;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import cz.incad.kramerius.utils.conf.KConfiguration;

public class PDFGenerateViewObject extends AbstractPrintViewObject {


    
    public String getNumberOfGeneratedPages() throws IOException, RecognitionException, TokenStreamException, ParserConfigurationException, SAXException {
        ResourceBundle bundle = this.resourceBundleService.getResourceBundle("labels", this.locale);
        return bundle.getString("pdf.numberOfPages");
    }

    public String getMaxNumberOfPages() {
        return ""+KConfiguration.getInstance().getConfiguration().getInt("generatePdfMaxRange");
    }
    
}
