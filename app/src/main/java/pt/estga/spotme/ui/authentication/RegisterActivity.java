package pt.estga.spotme.ui.authentication;

import android.content.Intent;
import android.os.Bundle;
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
import pt.estga.spotme.utils.UserSession;
import pt.estga.spotme.utils.PasswordUtils; // Classe para Hashing
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText, phoneEditText;
    private Button registerButton;
    private TextView loginTextView;
    private UserDao userDao;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_view);

        // Inicializar BD
        AppDatabase db = AppDatabase.getInstance(this);
        userDao = db.userDao();

        // Inicializar os elementos da UI
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        phoneEditText = findViewById(R.id.phoneNumEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginText);

        // Ação do botão de registo
        registerButton.setOnClickListener(v -> registerUser());

        // Ir para a página de login
        loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()) {
            showToast("Todos os campos devem ser preenchidos!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showToast("As senhas não coincidem!");
            return;
        }

        // Verifica se o utilizador já existe e regista-o
        executorService.execute(() -> {
            User existingUser = userDao.getUserByEmail(email);
            if (existingUser != null) {
                runOnUiThread(() -> showToast("Email já registado!"));
                return;
            }

            // Hash da senha antes de salvar
            String hashedPassword = PasswordUtils.hashPassword(password);

            // Criar e inserir novo usuário na BD
            User newUser = new User(username, hashedPassword, email, phone);
            userDao.insert(newUser);

            // Procurar o ID do novo utilizador
            User registeredUser = userDao.getUserByEmail(email);
            if (registeredUser != null) {
                UserSession session = UserSession.getInstance(getApplicationContext());
                session.setUser(registeredUser);
                session.setUserProfileImage(registeredUser.getProfileImage()); // Caso tenha imagem
                session.setUserPassword(password);

                runOnUiThread(() -> {
                    showToast("Conta registada com sucesso!");
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                });
            }
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
