package com.emirtechs.myjourneys.roomdb;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.emirtechs.myjourneys.model.Place;

@Database(entities = {Place.class},version = 1)
public abstract class PlaceDatabase extends RoomDatabase {
    public abstract PlaceDao placeDao();


}
