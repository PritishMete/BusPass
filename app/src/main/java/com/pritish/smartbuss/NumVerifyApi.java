package com.pritish.smartbuss;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NumVerifyApi {
    @GET("validate")
    Call<PhoneValidationResponse> validatePhoneNumber(
            @Query("access_key") String apiKey,
            @Query("number") String phoneNumber
    );
}

