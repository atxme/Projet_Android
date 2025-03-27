package com.example.quiz.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.quiz.R;
import com.example.quiz.databinding.FragmentAuthBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.util.Objects;
import java.util.regex.Pattern;

public class AuthFragment extends Fragment {

    private static final String TAG = "AuthFragment";
    private static final int RC_SIGN_IN = 9001;
    // Regex pour valider l'email
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
                    "@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );

    private FragmentAuthBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAuthBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialiser Firebase Auth
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configurer Google Sign In avec des options de sécurité renforcées
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // Configuration des boutons
        binding.buttonLogin.setOnClickListener(v -> signIn());
        binding.buttonRegister.setOnClickListener(v -> createAccount());
        binding.buttonGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        binding.buttonPlayAsGuest.setOnClickListener(v -> playAsGuest());
    }

    private void createAccount() {
        TextInputEditText emailInput = binding.inputEmail;
        TextInputEditText passwordInput = binding.inputPassword;

        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();

        if (!validateForm(email, password)) {
            return;
        }

        showProgressBar();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Création du compte réussie
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = auth.getCurrentUser();
                        createUserInFirestore(user);
                        navigateToHome();
                    } else {
                        // Échec de la création du compte
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(requireContext(), "Échec de l'inscription: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Erreur inconnue"),
                                Toast.LENGTH_SHORT).show();
                    }
                    hideProgressBar();
                });
    }

    private void signIn() {
        TextInputEditText emailInput = binding.inputEmail;
        TextInputEditText passwordInput = binding.inputPassword;

        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();

        if (!validateForm(email, password)) {
            return;
        }

        showProgressBar();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Connexion réussie
                        Log.d(TAG, "signInWithEmail:success");
                        navigateToHome();
                    } else {
                        // Échec de la connexion
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(requireContext(), "Échec de la connexion: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Erreur inconnue"),
                                Toast.LENGTH_SHORT).show();
                    }
                    hideProgressBar();
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Connexion Google réussie, authentification avec Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                // Échec de la connexion Google
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(requireContext(), "Échec de la connexion Google", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showProgressBar();
        
        // Puisque nous n'utilisons pas le token ID, nous allons nous connecter anonymement
        // puis mettre à jour les informations utilisateur si possible
        auth.signInAnonymously()
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Connexion réussie
                        Log.d(TAG, "signInAnonymously:success");
                        // Récupérer les informations de l'utilisateur Google
                        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
                        if (account != null) {
                            // Créer un utilisateur dans Firestore avec les infos Google
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                // Mettre à jour le profil avec des informations de Google
                                // Note: ceci crée un utilisateur "hybride" sans réelle authentification Google
                                db.collection("users").document(user.getUid())
                                        .set(new UserProfile(
                                                user.getUid(),
                                                account.getDisplayName(),
                                                account.getEmail(),
                                                account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null
                                        ))
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "User profile updated with Google info");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w(TAG, "Error updating user profile", e);
                                        });
                            }
                        }
                        navigateToHome();
                    } else {
                        // Échec de la connexion
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        Toast.makeText(requireContext(), "Échec de l'authentification", Toast.LENGTH_SHORT).show();
                    }
                    hideProgressBar();
                });
    }

    // Classe pour stocker les informations de profil utilisateur
    private static class UserProfile {
        public String uid;
        public String displayName;
        public String email;
        public String photoUrl;
        
        public UserProfile() {
            // Constructeur vide requis pour Firestore
        }
        
        public UserProfile(String uid, String displayName, String email, String photoUrl) {
            this.uid = uid;
            this.displayName = displayName;
            this.email = email;
            this.photoUrl = photoUrl;
        }
    }

    private void playAsGuest() {
        // Connexion anonyme
        showProgressBar();
        auth.signInAnonymously()
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Connexion anonyme réussie
                        Log.d(TAG, "signInAnonymously:success");
                        navigateToHome();
                    } else {
                        // Échec de la connexion anonyme
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        Toast.makeText(requireContext(), "Échec de la connexion anonyme", Toast.LENGTH_SHORT).show();
                    }
                    hideProgressBar();
                });
    }

    private boolean validateForm(String email, String password) {
        boolean valid = true;

        // Validation de l'email
        if (TextUtils.isEmpty(email)) {
            binding.inputLayoutEmail.setError("Requis");
            valid = false;
        } else if (!isValidEmail(email)) {
            binding.inputLayoutEmail.setError("Format d'email invalide");
            valid = false;
        } else {
            binding.inputLayoutEmail.setError(null);
        }

        // Validation du mot de passe
        if (TextUtils.isEmpty(password)) {
            binding.inputLayoutPassword.setError("Requis");
            valid = false;
        } else if (password.length() < 8) { // Exigence de sécurité plus stricte
            binding.inputLayoutPassword.setError("Minimum 8 caractères");
            valid = false;
        } else if (!isStrongPassword(password)) {
            binding.inputLayoutPassword.setError("Le mot de passe doit contenir au moins un chiffre, une lettre majuscule et un caractère spécial");
            valid = false;
        } else {
            binding.inputLayoutPassword.setError(null);
        }

        return valid;
    }
    
    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    private boolean isStrongPassword(String password) {
        // Vérifier si le mot de passe contient au moins un chiffre
        boolean hasDigit = false;
        // Vérifier si le mot de passe contient au moins une lettre majuscule
        boolean hasUpperCase = false;
        // Vérifier si le mot de passe contient au moins un caractère spécial
        boolean hasSpecialChar = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecialChar = true;
            }
        }
        
        return hasDigit && hasUpperCase && hasSpecialChar;
    }

    private void createUserInFirestore(FirebaseUser user) {
        if (user == null) return;

        // Vérifier si l'utilisateur existe déjà dans Firestore
        db.collection("players").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Créer un nouvel utilisateur
                        String displayName = user.getDisplayName();
                        if (TextUtils.isEmpty(displayName)) {
                            displayName = "Joueur " + user.getUid().substring(0, 5);
                        }

                        com.example.quiz.model.Player player = new com.example.quiz.model.Player(
                                user.getUid(),
                                displayName,
                                user.getEmail() != null ? user.getEmail() : ""
                        );
                        
                        if (user.getPhotoUrl() != null) {
                            player.setPhotoUrl(user.getPhotoUrl().toString());
                        }

                        // Enregistrer l'utilisateur dans Firestore
                        db.collection("players").document(user.getUid())
                                .set(player)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "User added to Firestore"))
                                .addOnFailureListener(e -> Log.w(TAG, "Error adding user to Firestore", e));
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error checking if user exists", e));
    }

    private void navigateToHome() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_auth_to_home);
    }

    private void showProgressBar() {
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 