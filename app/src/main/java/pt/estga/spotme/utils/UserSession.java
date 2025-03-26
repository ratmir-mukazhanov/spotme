package pt.estga.spotme.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import pt.estga.spotme.entities.User;

public class UserSession {

    private static UserSession instance;
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    // Keys para armazenar dados do utilizador
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_USER_PASSWORD = "userPassword"; // Se precisares armazenar senha
    private static final String KEY_USER_PROFILE_IMAGE = "userProfileImage";

    // Construtor privado (Singleton)
    private UserSession(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPreferences.edit();
    }

    public static synchronized UserSession getInstance(Context context) {
        if (instance == null) {
            instance = new UserSession(context.getApplicationContext());
        }
        return instance;
    }

    // Salvar todos os detalhes do utilizador
    public void setUser(User user) {
        editor.putLong(KEY_USER_ID, user.getId());
        editor.putString(KEY_USER_NAME, user.getUsername());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putString(KEY_USER_PHONE, user.getPhone());
        editor.putString(KEY_USER_PASSWORD, user.getPassword()); // Apenas se necessário
        editor.putString(KEY_USER_PROFILE_IMAGE, user.getProfileImage());
        editor.apply();
    }

    // Obter o objeto completo do utilizador
    public User getUser() {
        long userId = sharedPreferences.getLong(KEY_USER_ID, -1);
        String userName = sharedPreferences.getString(KEY_USER_NAME, null);
        String userEmail = sharedPreferences.getString(KEY_USER_EMAIL, null);
        String userPhone = sharedPreferences.getString(KEY_USER_PHONE, null);
        String userPassword = sharedPreferences.getString(KEY_USER_PASSWORD, null); // Se necessário
        String userProfileImage = sharedPreferences.getString(KEY_USER_PROFILE_IMAGE, null);

        // Verifica se os dados do utilizador estão completos
        if (userId != -1 && userName != null && userEmail != null) {
            User user = new User(userName, userPassword, userEmail, userPhone);
            user.setId(userId);
            user.setProfileImage(userProfileImage);
            return user;
        } else {
            return null;
        }
    }

    // Métodos individuais para definir e recuperar os atributos
    public void setUserId(long userId) {
        editor.putLong(KEY_USER_ID, userId).apply();
    }

    public long getUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, -1);
    }

    public void setUserName(String userName) {
        editor.putString(KEY_USER_NAME, userName).apply();
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, null);
    }

    public void setUserEmail(String userEmail) {
        editor.putString(KEY_USER_EMAIL, userEmail).apply();
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    public void setUserPhone(String userPhone) {
        editor.putString(KEY_USER_PHONE, userPhone).apply();
    }

    public String getUserPhone() {
        return sharedPreferences.getString(KEY_USER_PHONE, null);
    }

    public void setUserPassword(String userPassword) {
        editor.putString(KEY_USER_PASSWORD, userPassword).apply();
    }

    public String getUserPassword() {
        return sharedPreferences.getString(KEY_USER_PASSWORD, null);
    }

    public void setUserProfileImage(String profileImage) {
        editor.putString(KEY_USER_PROFILE_IMAGE, profileImage).apply();
    }

    public String getUserProfileImage() {
        return sharedPreferences.getString(KEY_USER_PROFILE_IMAGE, null);
    }

    // Limpar sessão do utilizador
    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
