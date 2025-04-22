package com.example.quiz.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.quiz.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private ImageView userProfileImage;
    private TextView userName, userEmail, userId;
    private Button logoutButton;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize views
        userProfileImage = view.findViewById(R.id.user_profile_image);
        userName = view.findViewById(R.id.user_name);
        userEmail = view.findViewById(R.id.user_email);
        userId = view.findViewById(R.id.user_id);
        logoutButton = view.findViewById(R.id.logout_button);
        
        // Display user information
        displayUserInfo();
        
        // Set logout button click listener
        logoutButton.setOnClickListener(v -> logout());
    }
    
    private void displayUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        
        if (user != null) {
            // Set user display name
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userName.setText(displayName);
            } else {
                userName.setText("Utilisateur");
            }
            
            // Set user email
            String email = user.getEmail();
            if (email != null && !email.isEmpty()) {
                userEmail.setText(email);
            } else {
                userEmail.setText("Email non disponible");
            }
            
            // Set user ID
            userId.setText("ID: " + user.getUid());
            
            // Set user profile image
            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(userProfileImage);
            }
        } else {
            // User is not logged in
            userName.setText("Invité");
            userEmail.setText("Non connecté");
            userId.setText("");
        }
    }
    
    private void logout() {
        mAuth.signOut();
        Toast.makeText(requireContext(), "Déconnecté", Toast.LENGTH_SHORT).show();
        
        // Navigate to auth screen
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.authFragment);
    }
} 