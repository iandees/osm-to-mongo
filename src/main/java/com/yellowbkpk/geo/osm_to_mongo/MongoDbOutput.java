package com.yellowbkpk.geo.osm_to_mongo;

import java.net.UnknownHostException;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class MongoDbOutput {

    private DB db;

    public MongoDbOutput() {
        try {
            Mongo m = new Mongo( "localhost" , 27017 );
            db = m.getDB( "osm" );
            
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (MongoException e) {
            e.printStackTrace();
        }
    }

    public void addNodes(List<DBObject> records) {
        DBCollection nodes = db.getCollection("nodes");
        nodes.insert(records);
    }
    
}
