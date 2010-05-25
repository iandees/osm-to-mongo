package com.yellowbkpk.geo.osm_to_mongo;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.mongodb.BasicDBObject;

public class OsmXmlParser {

    private MongoDbOutput output;

    public OsmXmlParser(MongoDbOutput output) {
        this.output = output;
    }

    public void parse(String filename) throws ParserConfigurationException, SAXException, IOException {
        OsmHandler osmHandler = new OsmHandler(output);
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(new File(filename), osmHandler);
    }
    
    private static final DateTimeFormatter XML_DATE_TIME_FORMAT =
        ISODateTimeFormat.dateTimeNoMillis();
    class OsmHandler extends DefaultHandler {

        private MongoDbOutput output;
        private BasicDBObject record;

        public OsmHandler(MongoDbOutput output) {
            this.output = output;
        }

        @SuppressWarnings("unchecked")
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            if("node".equals(localName)) {
                fillDefaults(attributes);
                fillLoc(attributes);
                record.append("ways", new LinkedList<Long>());
                record.append("relations", new LinkedList<Long>());
            } else if("nd".equals(localName)) {
                ((List<Long>) record.get("nodes")).add(Long.valueOf(attributes.getValue("ref")));
            } else if("tag".equals(localName)) {
                ((List<BasicDBObject>) record.get("tags")).add(new BasicDBObject(attributes.getValue("k"), attributes.getValue("v")));
            } else if("way".equals(localName)) {
                fillDefaults(attributes);
                record.append("nodes", new LinkedList<Long>());
                record.append("relations", new LinkedList<Long>());
            } else if("relation".equals(localName)) {
                fillDefaults(attributes);
            }
        }
        
        public void endElement(String uri, String localName, String qName)
        throws SAXException {
        }

        private void fillLoc(Attributes attributes) {
            BasicDBObject twoD = new BasicDBObject();
            twoD.append("lat", Float.parseFloat(attributes.getValue("lat")));
            twoD.append("lon", Float.parseFloat(attributes.getValue("lon")));
            record.append("loc", new BasicDBObject("loc", twoD));
        }

        private void fillDefaults(Attributes attributes) {
            record.append("id", Long.valueOf(attributes.getValue("id")));
            record.append("timestamp", parseIsoTime(attributes.getValue("timestamp")));
            record.append("tags", new LinkedList<BasicDBObject>());
            applyIfNotNull(attributes, "user");
            applyIfNotNull(attributes, "uid");
            applyIfNotNull(attributes, "version");
            applyIfNotNull(attributes, "changeset");
            
        }

        private void applyIfNotNull(Attributes attributes, String attr) {
            String string = attributes.getValue(attr);
            if(string != null) {
                record.append(attr, string);
            }
        }

        private DateTime parseIsoTime(String value) {
            return XML_DATE_TIME_FORMAT.parseDateTime(value);
        }
    }

}
