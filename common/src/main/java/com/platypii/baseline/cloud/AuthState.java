package com.platypii.baseline.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class AuthState {
    private static final String PREF_AUTH_USER = "auth_user";

    public static class SignedOut extends AuthState {
        @NonNull
        @Override
        public String toString() {
            return "SignedOut";
        }
    }
    public static class SigningIn extends AuthState {
        @NonNull
        @Override
        public String toString() {
            return "SigningIn";
        }
    }
    public static class SignedIn extends AuthState {
        final String userId;
        public SignedIn(String userId) {
            this.userId = userId;
        }
        @NonNull
        @Override
        public String toString() {
            return "SignedIn(" + userId + ")";
        }
    }

    // Save last sign in state so that sign in panel doesn't blink
    @Nullable
    public static AuthState currentAuthState = null;

    /**
     * Load currentAuthState from preferences, if needed
     */
    public static void loadFromPreferences(@NonNull Context context) {
        if (currentAuthState == null) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final String userId = prefs.getString(PREF_AUTH_USER, null);
            if (userId != null) {
                currentAuthState = new SignedIn(userId);
            } else {
                currentAuthState = new SignedOut();
            }
        }
    }

    @Nullable
    public static String getUser() {
        if (currentAuthState instanceof SignedIn) {
            return ((SignedIn) currentAuthState).userId;
        } else {
            return null;
        }
    }

    public static void setState(@NonNull Context context, @NonNull AuthState state) {
        currentAuthState = state;
        final String userId;
        if (currentAuthState instanceof SignedIn) {
            userId = ((SignedIn) currentAuthState).userId;
        } else {
            userId = null;
        }
        // Save to preferences
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_AUTH_USER, userId);
        editor.apply();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AuthState && obj.toString().equals(toString());
    }

}