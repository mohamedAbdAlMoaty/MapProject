package com.example.map.persistence;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.map.models.GooglePlace;

import java.util.List;

@Dao
public interface GooglePlaceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<GooglePlace> googlePlacesList);

    @Query("SELECT * FROM google_place_table ORDER BY placeName ASC")
    LiveData<List<GooglePlace>> getAllGooglePlaces();
}