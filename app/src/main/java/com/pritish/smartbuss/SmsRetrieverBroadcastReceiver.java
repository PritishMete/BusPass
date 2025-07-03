//package com.pritish.smartbuss;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//
//import com.google.android.gms.auth.api.phone.SmsRetriever;
//import com.google.android.gms.common.api.CommonStatusCodes;
//import com.google.android.gms.common.api.Status;
//
//public class SmsRetrieverBroadcastReceiver extends BroadcastReceiver {
//
//    private static final String TAG = "SmsBroadcastReceiver";
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
//            Bundle extras = intent.getExtras();
//            Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
//
//            switch (status.getStatusCode()) {
//                case CommonStatusCodes.SUCCESS:
//                    // Get SMS message content
//                    String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
//                    Log.d(TAG, "Retrieved SMS: " + message);
//
//                    // Pass the message to the signup activity
//                    // This requires the signup activity to be running
//                    if (context instanceof signup) {
//                        ((signup) context).setOtpFromSms(message);
//                    } else {
//                        // Try to send the message via an intent to the signup activity
//                        Intent signupIntent = new Intent(context, signup.class);
//                        signupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        signupIntent.putExtra("SMS_MESSAGE", message);
//                        context.startActivity(signupIntent);
//                    }
//                    break;
//                case CommonStatusCodes.TIMEOUT:
//                    // Timeout reached - SMS was not retrieved
//                    Log.d(TAG, "SMS retrieval timed out");
//                    break;
//            }
//        }
//    }
//}