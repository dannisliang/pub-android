package com.vandyapps.pubandroid;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Query;

public interface PubService {
    @GET("/order")
    OrderResponse getOrders(@Query("count") int count, @Query("apikey") String apikey);
}
