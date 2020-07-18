package com.example.map.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.map.models.GooglePlace;
import com.example.map.repositories.PlaceRepository;

import java.util.List;

public class PlaceviewModel extends AndroidViewModel {
    private PlaceRepository placeRepository;
    private LiveData<List<GooglePlace>> googlePlacesLiveData;

    public PlaceviewModel(@NonNull Application application) {
        super(application);
        placeRepository = new PlaceRepository(application);
        googlePlacesLiveData = placeRepository.getGooglePlacesLiveData();
    }

    public void searchGooglePlaces(String query, boolean useSDK) {
        if (useSDK) {
            placeRepository.searchGooglePlacesWithSdk(query);
        } else {
            placeRepository.searchGooglePlacesWithApi(query);
        }
    }

    public LiveData<List<GooglePlace>> getGooglePlacesLiveData() {
        return googlePlacesLiveData;
    }
}
