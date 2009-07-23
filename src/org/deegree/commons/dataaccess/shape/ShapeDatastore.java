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

package org.deegree.commons.dataaccess.shape;

import static org.deegree.feature.types.property.GeometryPropertyType.CoordinateDimension.DIM_2_OR_3;
import static org.deegree.feature.types.property.GeometryPropertyType.GeometryType.GEOMETRY;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.namespace.QName;

import org.deegree.commons.dataaccess.dbase.DBFReader;
import org.deegree.commons.index.RTree;
import org.deegree.commons.index.SpatialIndex;
import org.deegree.commons.utils.Pair;
import org.deegree.crs.CRS;
import org.deegree.crs.exceptions.TransformationException;
import org.deegree.crs.exceptions.UnknownCRSException;
import org.deegree.crs.exceptions.WKTParsingException;
import org.deegree.feature.Feature;
import org.deegree.feature.FeatureCollection;
import org.deegree.feature.GenericFeature;
import org.deegree.feature.GenericFeatureCollection;
import org.deegree.feature.GenericProperty;
import org.deegree.feature.Property;
import org.deegree.feature.types.FeatureType;
import org.deegree.feature.types.GenericFeatureType;
import org.deegree.feature.types.property.GeometryPropertyType;
import org.deegree.feature.types.property.PropertyType;
import org.deegree.feature.types.property.SimplePropertyType;
import org.deegree.filter.Filter;
import org.deegree.filter.FilterEvaluationException;
import org.deegree.geometry.Envelope;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.GeometryTransformer;
import org.slf4j.Logger;

/**
 * <code>ShapeDatastore</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class ShapeDatastore {

    private static final Logger LOG = getLogger( ShapeDatastore.class );

    private SHPReader shp;

    private DBFReader dbf;

    private long shpLastModified, dbfLastModified;

    private File shpFile, dbfFile;

    private String name;

    private CRS crs;

    private Charset encoding;

    private boolean available = true;

    private GeometryTransformer transformer;

    private GenericFeatureType featureType;

    /**
     * @param name
     * @param crs
     * @param encoding
     * @throws FileNotFoundException
     * @throws IOException
     */
    public ShapeDatastore( String name, CRS crs, Charset encoding ) throws FileNotFoundException, IOException {
        if ( name.toLowerCase().endsWith( ".shp" ) ) {
            name = name.substring( 0, name.length() - 4 );
        }

        LOG.debug( "Loading shape file '{}'", name );

        this.name = name;
        this.crs = crs;
        this.encoding = encoding;

        if ( crs == null ) {
            File prj = new File( name + ".PRJ" );
            if ( !prj.exists() ) {
                prj = new File( name + ".prj" );
            }
            if ( prj.exists() ) {
                try {
                    crs = new CRS( prj );
                } catch ( WKTParsingException e ) {
                    BufferedReader in = new BufferedReader( new FileReader( prj ) );
                    String c = in.readLine().trim();
                    try {
                        crs = new CRS( c );
                        crs.getWrappedCRS(); // resolve NOW
                        LOG.debug( ".prj contained EPSG code '{}'", crs.getName() );
                    } catch ( UnknownCRSException e2 ) {
                        LOG.warn( "Could not parse the .prj projection file for {}, reason: {}.", name,
                                  e.getLocalizedMessage() );
                        LOG.warn( "The file also does not contain a valid EPSG code." );
                        LOG.trace( "Stack trace of failed WKT parsing:", e );
                        crs = new CRS( "EPSG:4326" );
                    }
                }
            } else {
                crs = new CRS( "EPSG:4326" );
            }
            try {
                transformer = new GeometryTransformer( crs.getWrappedCRS() );
            } catch ( IllegalArgumentException e ) {
                LOG.error( "Unknown error", e );
            } catch ( UnknownCRSException e ) {
                LOG.error( "Unknown error", e );
            }
        }

        this.crs = crs;

        shpFile = new File( name + ".SHP" );
        if ( !shpFile.exists() ) {
            shpFile = new File( name + ".shp" );
        }
        shpLastModified = shpFile.lastModified();
        dbfFile = new File( name + ".DBF" );
        if ( !dbfFile.exists() ) {
            dbfFile = new File( name + ".dbf" );
        }
        dbfLastModified = dbfFile.lastModified();

        shp = getSHP( false );

        try {
            dbf = new DBFReader( new RandomAccessFile( dbfFile, "r" ), encoding, new QName( new File( name ).getName() ) );
            featureType = dbf.getFeatureType();
        } catch ( IOException e ) {
            LOG.warn( "A dbf file was not loaded (no attributes will be available): {}.dbf", name );
            GeometryPropertyType geomProp = new GeometryPropertyType( new QName( "geometry" ), 0, 1, GEOMETRY,
                                                                      DIM_2_OR_3 );
            featureType = new GenericFeatureType( new QName( new File( name ).getName() ),
                                                  Collections.<PropertyType> singletonList( geomProp ), false );
        }
    }

    private SHPReader getSHP( boolean forceIndexRebuild )
                            throws IOException {
        shp = null;

        File rtfile = new File( name + ".rti" );
        if ( rtfile.exists() && !( rtfile.lastModified() < shpFile.lastModified() ) && !forceIndexRebuild ) {
            try {
                SpatialIndex<Long> rtree = new RTree<Long>( new FileInputStream( name + ".rti" ) );
                shp = new SHPReader( new RandomAccessFile( shpFile, "r" ), crs, rtree );
            } catch ( IOException e ) {
                LOG.debug( "Stack trace:", e );
                LOG.warn( "Existing rtree index could not be read. Generating a new one..." );
            } catch ( ClassNotFoundException e ) {
                LOG.debug( "Stack trace:", e );
                LOG.warn( "Existing rtree index could not be read. Generating a new one..." );
            }
            if ( shp != null ) {
                return shp;
            }
        }

        shp = new SHPReader( new RandomAccessFile( shpFile, "r" ), crs, null );

        LOG.debug( "Building rtree index in memory for '{}'", new File( name ).getName() );

        RTree<Long> rtree = createIndex( shp );
        shp.close();
        LOG.debug( "done." );
        shp = new SHPReader( new RandomAccessFile( shpFile, "r" ), crs, rtree );
        RandomAccessFile output = new RandomAccessFile( name + ".rti", "rw" );
        rtree.write( output );
        output.close();
        return shp;
    }

    /**
     * @param shapeReader
     * @throws IOException
     */
    private RTree<Long> createIndex( SHPReader shapeReader )
                            throws IOException {
        Envelope env = shapeReader.getEnvelope();
        // use 128 values per rect.
        RTree<Long> result = new RTree<Long>( env, -1 );
        // to work around Java's non-existent variant type
        LOG.debug( "Read envelopes from shape file..." );
        ArrayList<Pair<float[], Long>> list = shapeReader.readEnvelopes();
        LOG.debug( "done." );
        result.buildIndex( list );
        return result;

    }

    private void checkForUpdate() {
        try {
            synchronized ( shpFile ) {
                if ( shpLastModified != shpFile.lastModified() ) {
                    shp.close();
                    LOG.debug( "Re-opening the shape file {}", name );
                    shp = getSHP( true );
                    shpLastModified = shpFile.lastModified();
                }
            }
            synchronized ( dbfFile ) {
                if ( dbf != null && dbfLastModified != dbfFile.lastModified() ) {
                    dbf.close();
                    LOG.debug( "Re-opening the dbf file {}", name );
                    dbf = new DBFReader( new RandomAccessFile( dbfFile, "r" ), encoding, new QName( name ) );
                    featureType = dbf.getFeatureType();
                    dbfLastModified = dbfFile.lastModified();
                }
            }
        } catch ( IOException e ) {
            available = false;
            LOG.debug( "Shape file {} is unavailable at the moment: {}", name, e.getLocalizedMessage() );
            LOG.trace( "Stack trace was {}", e );
        }
    }

    /**
     * @param bbox
     * @return the bbox in the native srs
     */
    public Envelope getTransformedEnvelope( Envelope bbox ) {
        if ( bbox != null && transformer != null ) {
            try {
                bbox = (Envelope) transformer.transform( bbox );
            } catch ( IllegalArgumentException e ) {
                LOG.error( "Unknown error", e );
            } catch ( TransformationException e ) {
                LOG.error( "Unknown error", e );
            } catch ( UnknownCRSException e ) {
                LOG.error( "Unknown error", e );
            }
        }
        return bbox;
    }

    /**
     * @param filter
     * @param bbox
     *            if the bbox filter is contained in the filter, it will be evaluated by deegree, if given here, the
     *            backend will do it
     * @param withGeometries
     *            whether to return geometry properties or not
     * @param exact 
     * @return a feature collection with matching features
     * @throws IOException
     * @throws FilterEvaluationException
     */
    public FeatureCollection query( Filter filter, Envelope bbox, boolean withGeometries, boolean exact )
                            throws IOException, FilterEvaluationException {

        checkForUpdate();

        if ( !available ) {
            return null;
        }

        bbox = getTransformedEnvelope( bbox );

        LinkedList<Pair<Integer, Geometry>> list;
        synchronized ( shp ) {
            list = shp.query( bbox, withGeometries, exact );
        }

        LOG.debug( "Got {} geometries", list.size() );

        LinkedList<Feature> feats = new LinkedList<Feature>();
        LinkedList<PropertyType> fields;
        if ( dbf == null ) {
            fields = new LinkedList<PropertyType>();
        } else {
            synchronized ( dbf ) {
                fields = dbf.getFields();
            }
        }
        GeometryPropertyType geom = (GeometryPropertyType) featureType.getPropertyDeclarations().get(
                                                                                                      featureType.getPropertyDeclarations().size() - 1 );
        if ( withGeometries ) {
            fields.add( geom );
        }

        if ( withGeometries ) {
            fields.removeLast();
        }

        while ( !list.isEmpty() ) {
            Pair<Integer, Geometry> pair = list.poll();
            HashMap<SimplePropertyType, Property<?>> entry;
            if ( dbf != null ) {
                synchronized ( dbf ) {
                    entry = dbf.getEntry( pair.first );
                }
            } else {
                entry = new HashMap<SimplePropertyType, Property<?>>();
            }
            LinkedList<Property<?>> props = new LinkedList<Property<?>>();
            for ( PropertyType t : fields ) {
                if ( entry.containsKey( t ) ) {
                    props.add( entry.get( t ) );
                }
            }
            if ( withGeometries ) {
                props.add( new GenericProperty<Geometry>( geom, pair.second ) );
            }
            GenericFeature feat = new GenericFeature( featureType, "shp_" + pair.first, props );

            if ( filter == null || filter.evaluate( feat ) ) {
                feats.add( feat );
            }
        }

        return new GenericFeatureCollection( null, feats );
    }

    /**
     * @return the envelope of the shape file
     */
    public Envelope getEnvelope() {
        checkForUpdate();
        if ( shp == null ) {
            return null;
        }
        return shp.getEnvelope();
    }

    /**
     * @throws IOException
     */
    public void close()
                            throws IOException {
        shp.close();
        if ( dbf != null ) {
            dbf.close();
        }
    }

    /**
     * @return whether the shape file is currently available
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * @return the crs of the data store. May be parsed from WKT!
     */
    public CRS getCRS() {
        return crs;
    }

    /**
     * @return the 'interpolated' feature type from the dbf
     */
    public FeatureType getFeatureType() {
        return featureType;
    }

}
