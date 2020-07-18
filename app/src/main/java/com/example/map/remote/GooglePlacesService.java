package com.example.map.remote;

import com.example.map.models.GooglePlacePrediction;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GooglePlacesService {
    @GET("/maps/api/place/autocomplete/json")
    Call<GooglePlacePrediction> autocompletePlace(@Query("input") String input, @Query("key") String key);
}
