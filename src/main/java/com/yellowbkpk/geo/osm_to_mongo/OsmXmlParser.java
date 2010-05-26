package com.yellowbkpk.geo.osm_to_mongo;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

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

        private static final int ACCUMULATION = 1;
        private MongoDbOutput output;
        private BasicDBObject record = new BasicDBObject();
        private List<DBObject> records = new LinkedList<DBObject>();

        public OsmHandler(MongoDbOutput output) {
            this.output = output;
        }

        @SuppressWarnings("unchecked")
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            if("node".equals(qName)) {
                fillDefaults(attributes);
                fillLoc(attributes);
                record.append("ways", new LinkedList<Long>());
                record.append("relations", new LinkedList<Long>());
            } else if("nd".equals(qName)) {
                ((List<Long>) record.get("nodes")).add(Long.valueOf(attributes.getValue("ref")));
            } else if("tag".equals(qName)) {
                BasicDBObject tagList = (BasicDBObject) record.get("tags");
                tagList.append(clean(attributes.getValue("k")),
                                clean(attributes.getValue("v")));
            } else if("nd".equals(qName)) {
                List<Long> refList = (List<Long>) record.get("nodes");
                refList.add(Long.parseLong(attributes.getValue("ref")));
            } else if("member".equals(qName)) {
                BasicDBObject member = new BasicDBObject();
                member.append("type", attributes.getValue("type"));
                member.append("ref", Long.parseLong(attributes.getValue("ref")));
                member.append("role", attributes.getValue("role"));
                ((List<BasicDBObject>) record.get("members")).add(member);
            } else if("way".equals(qName)) {
                fillDefaults(attributes);
                record.append("nodes", new LinkedList<Long>());
                record.append("relations", new LinkedList<Long>());
            } else if("relation".equals(qName)) {
                fillDefaults(attributes);
                record.append("members", new LinkedList<BasicDBObject>());
            }
        }

        private String clean(String attributes) {
            return attributes.replaceAll("\\.", ",,");
        }

        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if ("node".equals(qName)) {
                records.add(record);
                record = new BasicDBObject();
                if (records.size() > ACCUMULATION) {
                    output.addNodes(records);
                    records.clear();
                }
            } else if ("way".equals(qName)) {
                output.addWay(record);
                record = new BasicDBObject();
            } else if ("relation".equals(qName)) {
                output.addRelation(record);
                record = new BasicDBObject();
            }
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
            record.append("tags", new BasicDBObject());
            applyIfNotNull(attributes, "user");
            applyNumIfNotNull(attributes, "uid");
            applyNumIfNotNull(attributes, "version");
            applyNumIfNotNull(attributes, "changeset");
            
        }

        private void applyIfNotNull(Attributes attributes, String attr) {
            String string = attributes.getValue(attr);
            if(string != null) {
                record.append(attr, string);
            }
        }

        private void applyNumIfNotNull(Attributes attributes, String attr) {
            String string = attributes.getValue(attr);
            if(string != null) {
                record.append(attr, Integer.parseInt(string));
            }
        }

        private long parseIsoTime(String value) {
            return XML_DATE_TIME_FORMAT.parseDateTime(value).getMillis();
        }
    }

}
