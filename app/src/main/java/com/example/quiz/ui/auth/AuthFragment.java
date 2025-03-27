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
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
// import com.google.firebase.appcheck.FirebaseAppCheck;
// import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
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
    private NavController navController;

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

        // Configurer Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // Initialiser le NavController
        navController = Navigation.findNavController(view);

        // Configuration des boutons
        binding.buttonGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        binding.buttonPlayAsGuest.setOnClickListener(v -> playAsGuest());
    }

    private void signInWithGoogle() {
        showProgressBar();
        // Utiliser la méthode traditionnelle de connexion Google
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Connexion Google réussie
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google sign in succeeded");
                
                if (account != null) {
                    // Si la connexion Google a fonctionné, essayer d'authentifier avec Firebase
                    if (account.getIdToken() != null) {
                        // Essayer d'authentifier avec Firebase
                        tryFirebaseAuthWithGoogle(account.getIdToken(), account.getEmail(), account.getDisplayName());
                    } else {
                        // Si pas de token ID, simuler une connexion avec les infos Google
                        simulateGoogleSignIn(account);
                    }
                }
            } catch (ApiException e) {
                // Échec de la connexion Google
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(requireContext(), "Échec de la connexion Google: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                
                // Si Google échoue, utiliser le mode invité
                playAsGuest();
            }
        }
    }
    
    private void simulateGoogleSignIn(GoogleSignInAccount account) {
        // Utiliser directement le mode hors-ligne avec les informations Google
        Log.d(TAG, "Using offline mode with Google account info");
        
        Toast.makeText(requireContext(), "Connecté en tant que: " + 
                (account != null ? account.getDisplayName() : "Invité"), Toast.LENGTH_SHORT).show();
        
        // Naviguer directement vers l'écran d'accueil
        navigateToHome();
        hideProgressBar();
    }

    private void createUserProfileInFirestore(FirebaseUser user) {
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("uid", user.getUid());
        userProfile.put("email", user.getEmail());
        userProfile.put("displayName", user.getDisplayName());
        userProfile.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        userProfile.put("createdAt", System.currentTimeMillis());
        
        db.collection("users").document(user.getUid())
                .set(userProfile)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile created or updated"))
                .addOnFailureListener(e -> Log.w(TAG, "Error creating user profile", e));
    }

    private void playAsGuest() {
        showProgressBar();
        
        // Utiliser directement le mode hors-ligne au lieu de l'authentification anonyme
        Log.d(TAG, "Using guest mode (offline)");
        Toast.makeText(requireContext(), "Mode invité activé", Toast.LENGTH_SHORT).show();
        
        // Naviguer vers l'écran d'accueil
        navigateToHome();
        hideProgressBar();
    }

    private void navigateToHome() {
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

    private void tryFirebaseAuthWithGoogle(String idToken, String email, String displayName) {
        // Créer un credential Firebase
        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
        
        // S'authentifier avec Firebase
        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener(requireActivity(), task -> {
                if (task.isSuccessful()) {
                    // Authentification réussie
                    Log.d(TAG, "signInWithCredential:success");
                    FirebaseUser user = auth.getCurrentUser();
                    
                    // Créer ou mettre à jour le profil utilisateur
                    if (user != null) {
                        createUserProfileInFirestore(user);
                    }
                    
                    Toast.makeText(requireContext(), "Connecté en tant que: " + 
                            (displayName != null ? displayName : email), 
                            Toast.LENGTH_SHORT).show();
                    
                    // Naviguer vers l'écran d'accueil
                    navigateToHome();
                } else {
                    // Échec de l'authentification Firebase
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                    
                    // Simuler une connexion réussie en mode offline
                    simulateGoogleSignIn(null);
                }
                hideProgressBar();
            });
    }
} 