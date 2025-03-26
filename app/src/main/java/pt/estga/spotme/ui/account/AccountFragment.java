package pt.estga.spotme.ui.account;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import pt.estga.spotme.MainActivity;
import pt.estga.spotme.R;
import pt.estga.spotme.database.AppDatabase;
import pt.estga.spotme.database.UserDao;
import pt.estga.spotme.entities.User;
import pt.estga.spotme.ui.authentication.LoginActivity;
import pt.estga.spotme.utils.PasswordUtils;
import pt.estga.spotme.utils.UserSession;

import java.io.File;
import java.io.FileOutputStream;

// TO:DO - Implement the Change Image and Profile Statistics View

public class AccountFragment extends Fragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private UserSession userSession;
    private AppDatabase db;
    private UserDao userDAO;
    private ImageView profileImage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.profile_main_view, container, false);

        // Initialize UI components
        TextView tvAccountTitle = root.findViewById(R.id.tvAccountTitle);
        profileImage = root.findViewById(R.id.profileImage); // Use the class field
        TextView tvNomeLabel = root.findViewById(R.id.tvNomeLabel);
        TextView tvNome = root.findViewById(R.id.tvNome);
        ImageView ivEditNome = root.findViewById(R.id.ivEditNome);
        TextView tvTelemovel = root.findViewById(R.id.tvTelemovel);
        ImageView ivEditTelemovel = root.findViewById(R.id.ivEditTelemovel);
        TextView tvEmailLabel = root.findViewById(R.id.tvEmailLabel);
        TextView tvEmail = root.findViewById(R.id.tvEmail);
        ImageView ivEditEmail = root.findViewById(R.id.ivEditEmail);
        TextView tvChangePassword = root.findViewById(R.id.tvChangePassword);
        ImageView ivArrowPassword = root.findViewById(R.id.ivArrowPassword);
        TextView tvPersonalStats = root.findViewById(R.id.tvPersonalStats);
        ImageView ivArrowStats = root.findViewById(R.id.ivArrowStats);
        ImageView ivEditProfilePhoto = root.findViewById(R.id.ivEditProfilePhoto);
        TextView tvDeleteAccount = root.findViewById(R.id.tvDeleteAccount);

        userSession = UserSession.getInstance(requireContext());
        db = AppDatabase.getInstance(requireContext());
        userDAO = db.userDao();

        // Set initial values or listeners if needed
        tvNome.setText(userSession.getUserName());
        tvEmail.setText(userSession.getUserEmail());
        tvTelemovel.setText(userSession.getUserPhone());

        carregarImagemDePerfil(); // Load the profile image

        // Add any additional setup or listeners here

        ivEditNome.setOnClickListener(v -> {
            LayoutInflater inflater1 = LayoutInflater.from(requireContext());
            View dialogView = inflater1.inflate(R.layout.dialog_edit_name, null);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
            dialogBuilder.setView(dialogView);

            EditText etNewName = dialogView.findViewById(R.id.etNewName);
            Button btnSave = dialogView.findViewById(R.id.btnSave);

            ImageButton buttonClose = dialogView.findViewById(R.id.btnClose);
            Button buttonCancel = dialogView.findViewById(R.id.btnCancel);

            etNewName.setText(userSession.getUserName());

            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();

            btnSave.setOnClickListener(view -> {
                String newName = etNewName.getText().toString().trim();
                if (!newName.isEmpty()) {
                    userSession.setUserName(newName);
                    tvNome.setText(newName);
                    new Thread(() -> {
                        userDAO.updateNome(newName, userSession.getUserId());
                    }).start();
                    ((MainActivity) requireActivity()).updateUserName(newName); // Update the hamburger menu
                    Toast.makeText(requireContext(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                } else {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });

            buttonClose.setOnClickListener(view -> alertDialog.dismiss());
            buttonCancel.setOnClickListener(view -> alertDialog.dismiss());
        });

        ivEditEmail.setOnClickListener(v -> {
            LayoutInflater inflater2 = LayoutInflater.from(requireContext());
            View dialogView = inflater2.inflate(R.layout.dialog_edit_email, null);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
            dialogBuilder.setView(dialogView);

            EditText etNewEmail = dialogView.findViewById(R.id.etNewEmail);
            Button btnSave = dialogView.findViewById(R.id.btnSave);

            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

            etNewEmail.setText(userSession.getUserEmail());

            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();

            btnSave.setOnClickListener(view -> {
                String newEmail = etNewEmail.getText().toString().trim();
                if (!newEmail.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    new Thread(() -> {
                        User existingUser = userDAO.findByEmail(newEmail);
                        if (existingUser == null) {
                            userSession.setUserEmail(newEmail);
                            tvEmail.setText(newEmail);
                            userDAO.updateEmail(newEmail, userSession.getUserId());
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Email updated successfully", Toast.LENGTH_SHORT).show();
                                alertDialog.dismiss();
                            });
                        } else {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Email is already registered", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                    ((MainActivity) requireActivity()).updateUserEmail(newEmail); // Update the hamburger menu
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show();
                }
            });

            btnCancel.setOnClickListener(view -> alertDialog.dismiss());
            btnClose.setOnClickListener(view -> alertDialog.dismiss());

        });

        ivEditTelemovel.setOnClickListener(v -> {
            LayoutInflater inflater3 = LayoutInflater.from(requireContext());
            View dialogView = inflater3.inflate(R.layout.dialog_edit_phone, null);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
            dialogBuilder.setView(dialogView);

            EditText etNewPhone = dialogView.findViewById(R.id.etNewPhone);
            Button btnSave = dialogView.findViewById(R.id.btnSave);

            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();

            etNewPhone.setText(userSession.getUserPhone());

            btnSave.setOnClickListener(view -> {
                String newPhone = etNewPhone.getText().toString().trim();
                if (!newPhone.isEmpty()) {
                    userSession.setUserPhone(newPhone);
                    tvTelemovel.setText(newPhone);
                    new Thread(() -> {
                        userDAO.updatePhone(newPhone, userSession.getUserId());
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Phone number updated successfully", Toast.LENGTH_SHORT).show();
                            alertDialog.dismiss();
                        });
                    }).start();
                } else {
                    Toast.makeText(requireContext(), "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });

            btnCancel.setOnClickListener(view -> alertDialog.dismiss());
            btnClose.setOnClickListener(view -> alertDialog.dismiss());
        });

        tvChangePassword.setOnClickListener(v -> {
            LayoutInflater inflater4 = LayoutInflater.from(requireContext());
            View dialogView = inflater4.inflate(R.layout.dialog_change_password, null);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
            dialogBuilder.setView(dialogView);

            EditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
            EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
            EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
            Button btnSave = dialogView.findViewById(R.id.btnSave);

            ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);

            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();

            btnSave.setOnClickListener(view -> {
                String currentPassword = etCurrentPassword.getText().toString().trim();
                String newPassword = etNewPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(requireContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                } else if (!currentPassword.equals(userSession.getUserPassword())) {
                    Toast.makeText(requireContext(), "Current Password failed, try again", Toast.LENGTH_SHORT).show();
                } else if (newPassword.equals(confirmPassword)) {
                    String hashedPassword = PasswordUtils.hashPassword(newPassword);
                    new Thread(() -> {
                        userDAO.updatePassword(hashedPassword, userSession.getUserId());
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                            alertDialog.dismiss();
                            // ir para a tela de login
                            userSession.clearSession();
                            Intent intent = new Intent(requireContext(), LoginActivity.class);
                            startActivity(intent);
                            requireActivity().finish();
                        });
                    }).start();
                } else {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                }
            });

            btnClose.setOnClickListener(view -> alertDialog.dismiss());
            btnCancel.setOnClickListener(view -> alertDialog.dismiss());
        });

        ivEditProfilePhoto.setOnClickListener(v -> {
            LayoutInflater inflater5 = LayoutInflater.from(requireContext());
            View dialogView = inflater5.inflate(R.layout.dialog_change_profile_image, null);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
            dialogBuilder.setView(dialogView);

            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();

            LinearLayout layoutCamera = dialogView.findViewById(R.id.layoutCamera);
            LinearLayout layoutGallery = dialogView.findViewById(R.id.layoutGallery);
            LinearLayout layoutRemove = dialogView.findViewById(R.id.layoutRemove);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);

            layoutCamera.setOnClickListener(view -> {
                // Handle taking a photo
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
                alertDialog.dismiss();
            });

            layoutGallery.setOnClickListener(view -> {
                // Handle choosing from gallery
                Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
                alertDialog.dismiss();
            });

            layoutRemove.setOnClickListener(view -> {
                // Handle removing the photo
                userSession.setUserProfileImage(null);
                profileImage.setImageResource(R.drawable.ic_default_profile); // Set to default image
                new Thread(() -> {
                    userDAO.updateProfileImage(null, (int) userSession.getUserId());
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Profile image removed", Toast.LENGTH_SHORT).show();
                        ((MainActivity) requireActivity()).updateProfileImage(null); // Update the hamburger menu
                    });
                }).start();

                alertDialog.dismiss();
            });

            btnCancel.setOnClickListener(view -> alertDialog.dismiss());

        });

        tvDeleteAccount.setOnClickListener(view -> {

            LayoutInflater inflater6 = LayoutInflater.from(requireContext());
            View dialogView1 = inflater6.inflate(R.layout.dialog_delete_account, null);

            AlertDialog.Builder dialogBuilder1 = new AlertDialog.Builder(requireContext());
            dialogBuilder1.setView(dialogView1);

            EditText etPassword = dialogView1.findViewById(R.id.etPassword);
            Button btnDelete = dialogView1.findViewById(R.id.btnDelete);

            ImageButton btnClose = dialogView1.findViewById(R.id.btnClose);
            Button buttonCancelar = dialogView1.findViewById(R.id.btnCancel);

            AlertDialog alertDialog = dialogBuilder1.create();
            alertDialog.show();

            btnDelete.setOnClickListener(view1 -> {
                String password = etPassword.getText().toString().trim();
                if (password.isEmpty()) {
                    Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show();
                } else if (!password.equals(userSession.getUserPassword())) {
                    Toast.makeText(requireContext(), "Password failed, try again", Toast.LENGTH_SHORT).show();
                } else {
                    new Thread(() -> {
                        userDAO.delete(userSession.getUserId());
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            alertDialog.dismiss();
                            // ir para a tela de login
                            userSession.clearSession();
                            Intent intent = new Intent(requireContext(), LoginActivity.class);
                            startActivity(intent);
                            requireActivity().finish();
                        });
                    }).start();
                }
            });

            btnClose.setOnClickListener(view1 -> alertDialog.dismiss());
            buttonCancelar.setOnClickListener(view1 -> alertDialog.dismiss());

        });

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        // Obter o ID do utilizador da sessão
                        long userId = userSession.getUserId();
                        if (userId <= 0) {
                            Log.e("ProfileImage", "ID do utilizador inválido.");
                            return;
                        }

                        // Copiar a imagem para o armazenamento interno
                        String imagePath = copyImageToInternalStorage(selectedImageUri, userId);

                        if (imagePath != null) {
                            // Atualizar a UI com a nova imagem
                            profileImage.setImageURI(Uri.fromFile(new File(imagePath)));

                            // Guardar o novo caminho na sessão e na base de dados
                            userSession.setUserProfileImage(imagePath);
                            new Thread(() -> {
                                userDAO.updateProfileImage(imagePath, userId);
                                requireActivity().runOnUiThread(() -> {
                                    ((MainActivity) requireActivity()).updateProfileImage(imagePath); // Atualiza o menu
                                });
                            }).start();
                        } else {
                            Log.e("ProfileImage", "Erro ao copiar imagem para armazenamento interno.");
                        }
                    } catch (Exception e) {
                        Log.e("ProfileImage", "Erro ao processar a imagem", e);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "Permissão concedida!");
                carregarImagemDePerfil(); // Agora podemos carregar a imagem
            } else {
                Log.e("Permissions", "Permissão negada! Não será possível carregar imagens do armazenamento.");
                Toast.makeText(requireContext(), "Permissão necessária para carregar imagens do armazenamento.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Método corrigido para garantir que cada utilizador tem a sua própria imagem
    private String copyImageToInternalStorage(Uri imageUri, long userId) {
        ContextWrapper cw = new ContextWrapper(requireContext());
        File directory = cw.getDir("profile_images", Context.MODE_PRIVATE);
        if (!directory.exists()) {
            directory.mkdirs(); // Criar diretório se não existir
        }

        // Criar nome de ficheiro único baseado no ID do utilizador
        File imageFile = new File(directory, "profile_" + userId + ".jpg");

        try (FileOutputStream fos = new FileOutputStream(imageFile);
             java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri)) {

            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.flush();
                Log.d("ProfileImage", "Imagem copiada para: " + imageFile.getAbsolutePath());
                return imageFile.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e("ProfileImage", "Erro ao copiar imagem para armazenamento interno", e);
        }
        return null;
    }

    // Método para carregar corretamente a imagem de perfil do utilizador logado
    private void carregarImagemDePerfil() {
        long userId = userSession.getUserId();
        if (userId <= 0) {
            Log.e("ProfileImage", "ID do utilizador inválido.");
            profileImage.setImageResource(R.drawable.ic_default_profile);
            return;
        }

        String profileImagePath = userSession.getUserProfileImage();
        Log.d("ProfileImage", "Caminho guardado na sessão: " + profileImagePath);

        if (profileImagePath == null || profileImagePath.isEmpty()) {
            // Se não houver caminho na sessão, tenta buscar na base de dados
            new Thread(() -> {
                String dbImagePath = userDAO.getProfileImage(userSession.getUserId());
                requireActivity().runOnUiThread(() -> {
                    if (dbImagePath != null && !dbImagePath.isEmpty()) {
                        File imgFile = new File(dbImagePath);
                        if (imgFile.exists()) {
                            profileImage.setImageURI(Uri.fromFile(imgFile));
                        } else {
                            Log.e("ProfileImage", "Ficheiro não encontrado: " + dbImagePath);
                            profileImage.setImageResource(R.drawable.ic_default_profile);
                        }
                    } else {
                        Log.e("ProfileImage", "Nenhuma imagem encontrada na base de dados.");
                        profileImage.setImageResource(R.drawable.ic_default_profile);
                    }
                });
            }).start();
        } else {
            // Se o caminho está na sessão, carrega diretamente
            File imgFile = new File(profileImagePath);
            if (imgFile.exists()) {
                profileImage.setImageURI(Uri.fromFile(imgFile));
            } else {
                Log.e("ProfileImage", "Ficheiro não encontrado: " + profileImagePath);
                profileImage.setImageResource(R.drawable.ic_default_profile);
            }
        }
    }


}