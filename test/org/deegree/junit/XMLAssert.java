//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 and
 lat/lon GmbH

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
package org.deegree.junit;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.deegree.commons.xml.schema.PreparserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * This class contains static assert methods for XML validation. This class should only be used in JUnit tests and
 * <strong>not</strong> as a general schema validator, as it uses the JUnit specific mechanisms (e.g. <code>fail</code>)
 * to signal success or failure.
 * 
 * @author <a href="mailto:tonnhofer@lat-lon.de">Oliver Tonnhofer</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class XMLAssert {

    private static final Logger LOG = LoggerFactory.getLogger( XMLAssert.class );

    private static final Map<String, Schema> schemas = new HashMap<String, Schema>();

    private static Schema getSchema( String schemaLocation ) {
        synchronized ( schemas ) {
            if ( schemas.containsKey( schemaLocation ) ) {
                return schemas.get( schemaLocation );
            }
            Schema schema = createSchema( schemaLocation );
            schemas.put( schemaLocation, schema );
            return schema;
        }
    }

    private static Schema createSchema( String schemaLocation ) {
        try {
            URL schemaDoc = new URL( schemaLocation );
            SchemaFactory sf = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
            return sf.newSchema( schemaDoc );
        } catch ( MalformedURLException e ) {
            fail( "couldn't parse schema location url (" + schemaLocation + ")" );
        } catch ( SAXException e ) {
            fail( "couldn't parse schema (" + schemaLocation + "): " + e.getMessage() );
        }
        return null;
    }

    /**
     * Check if the input is valid against the xml schema.
     * <p>
     * The validator will cache the schema documents.
     * </p>
     * 
     * @param schemaLocation
     * @param source
     * @throws AssertionError
     *             when the document is not valid against the schema
     */
    public static void assertValidDocument( String schemaLocation, XMLInputSource source ) {

        final List<Exception> exceptions = new LinkedList<Exception>();

        try {
            XMLParserConfiguration parserConfig = PreparserHelper.getValidatingParser( schemaLocation );
            parserConfig.setErrorHandler(new XMLErrorHandler() {
                @Override
                public void error( String arg0, String arg1, XMLParseException e )
                                        throws XNIException {
                    exceptions.add( e );
                }

                @Override
                public void fatalError( String arg0, String arg1, XMLParseException e )
                                        throws XNIException {
                    exceptions.add( e );
                }

                @Override
                public void warning( String arg0, String arg1, XMLParseException e )
                                        throws XNIException {
                    exceptions.add( e );
                }
                
            });
            parserConfig.parse( source );
        } catch ( Exception e ) {
            exceptions.add( e );
        }
        if ( LOG.isErrorEnabled() ) {
            for ( Exception ex : exceptions ) {
                LOG.error( "Parsing error: {}", ex.getMessage(), ex );
            }
        }
        assertEquals( "catched some unexpected exceptions while validating against the schema. see error log", 0,
                      exceptions.size() );
    }
}
