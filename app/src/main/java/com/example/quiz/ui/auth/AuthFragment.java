package com.example.quiz.ui.auth;

import android.os.Bundle;
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

public class AuthFragment extends Fragment {

    private FragmentAuthBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAuthBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configuration des boutons
        binding.buttonLogin.setOnClickListener(v -> loginDemo());
        binding.buttonRegister.setOnClickListener(v -> registerDemo());
        binding.buttonGoogleSignIn.setOnClickListener(v -> googleSignInDemo());
        binding.buttonPlayAsGuest.setOnClickListener(v -> playAsGuest());
    }

    private void loginDemo() {
        Toast.makeText(requireContext(), "Connexion (demo) réussie", Toast.LENGTH_SHORT).show();
        navigateToHome();
    }

    private void registerDemo() {
        Toast.makeText(requireContext(), "Inscription (demo) réussie", Toast.LENGTH_SHORT).show();
        navigateToHome();
    }

    private void googleSignInDemo() {
        Toast.makeText(requireContext(), "Connexion Google (demo) réussie", Toast.LENGTH_SHORT).show();
        navigateToHome();
    }

    private void playAsGuest() {
        Toast.makeText(requireContext(), "Mode invité activé", Toast.LENGTH_SHORT).show();
        navigateToHome();
    }

    private void navigateToHome() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_auth_to_home);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 