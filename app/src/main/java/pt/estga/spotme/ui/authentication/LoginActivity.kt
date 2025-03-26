package pt.estga.spotme.ui.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import pt.estga.spotme.MainActivity;
import pt.estga.spotme.R;
import pt.estga.spotme.database.AppDatabase;
import pt.estga.spotme.database.UserDao;
import pt.estga.spotme.entities.User;
import pt.estga.spotme.utils.PasswordUtils;
import pt.estga.spotme.utils.UserSession;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView signUpButton;
    private UserDao userDao;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_view);

        // Inicializar elementos da UI
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signupText);

        // Inicializar a base de dados e o DAO
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        userDao = db.userDao();

        // Inicializar o executor para operações assíncronas
        executorService = Executors.newSingleThreadExecutor();

        // Definir ações dos botões
        loginButton.setOnClickListener(v -> loginUser());
        signUpButton.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validação dos campos
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("Preencha o email e a senha.");
            return;
        }

        // Desativar botão para evitar múltiplos cliques
        loginButton.setEnabled(false);

        // Autenticar utilizador em background
        executorService.execute(() -> authenticateUser(email, password));
    }

    private void authenticateUser(String email, String password) {
        User user = userDao.getUserByEmail(email);

        if (user == null) {
            runOnUiThread(() -> {
                showToast("Utilizador não encontrado.");
                loginButton.setEnabled(true);
            });
            return;
        }

        // Verificar a senha encriptada
        if (PasswordUtils.verifyPassword(password, user.getPassword())) {
            // Guardar o ID do user na sessão
            UserSession session = UserSession.getInstance(getApplicationContext());
            session.setUser(user);
            session.setUserProfileImage(user.getProfileImage()); // Atualizar imagem de perfil
            session.setUserPassword(password);


            runOnUiThread(() -> {
                showToast("Login bem-sucedido!");
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            });
        } else {
            runOnUiThread(() -> {
                showToast("Senha incorreta.");
                loginButton.setEnabled(true);
            });
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown(); // Fecha o executor para evitar vazamento de memória
    }
}
