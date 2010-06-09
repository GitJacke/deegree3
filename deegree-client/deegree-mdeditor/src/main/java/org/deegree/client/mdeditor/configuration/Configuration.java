//$HeadURL: svn+ssh://lbuesching@svn.wald.intevation.de/deegree/base/trunk/resources/eclipse/files_template.xml $
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.client.mdeditor.configuration;

import java.io.File;

/**
 * TODO add class documentation here
 * 
 * @author <a href="mailto:buesching@lat-lon.de">Lyn Buesching</a>
 * @author last edited by: $Author: lyn $
 * 
 * @version $Revision: $, $Date: $
 */
public class Configuration {

    // TODO!!!
    private static String formConfURL = "/home/lyn/workspace/deegree-mdeditor/resources/exampleConfiguration.xml";

    private static String codeListConfURL = "/home/lyn/workspace/deegree-mdeditor/resources/exampleCodeListConfiguration.xml";

    private static String filesDirURL = "/home/lyn/workspace/deegree-mdeditor/tmp/";

    private static String downloadDirURL = "/home/lyn/workspace/deegree-mdeditor/src/main/webapp/download/";

    public static void setFormConfURL( String formConfURL ) {
        Configuration.formConfURL = formConfURL;
    }

    public static String getFormConfURL() {
        return formConfURL;
    }

    public static void setFilesDirURL( String filesDirURL ) {
        Configuration.filesDirURL = filesDirURL;
    }

    public static String getFilesDirURL() {
        if ( !filesDirURL.endsWith( File.separator ) ) {
            return filesDirURL + File.separator;
        }
        return filesDirURL;
    }

    public static String getDownloadDirURL() {
        if ( !downloadDirURL.endsWith( File.separator ) ) {
            return downloadDirURL + File.separator;
        }
        return downloadDirURL;
    }

    public static String getCodeListURL() {
        return codeListConfURL;
    }

    public static void setCodeListURL( String codeListConfURL ) {
        Configuration.codeListConfURL = codeListConfURL;
    }

}
