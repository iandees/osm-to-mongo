package com.yellowbkpk.geo.osm_to_mongo;

import java.net.UnknownHostException;

import com.mongodb.DB;
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
    
}
