package com.example.map.persistence;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.map.models.GooglePlace;

@Database(entities = {GooglePlace.class}, version = 1, exportSchema = false)
public abstract class PlaceDatabase extends RoomDatabase {
    public abstract GooglePlaceDao googlePlaceDao();

    private static volatile PlaceDatabase INSTANCE;

    public static PlaceDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PlaceDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            PlaceDatabase.class,
                            "place_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
