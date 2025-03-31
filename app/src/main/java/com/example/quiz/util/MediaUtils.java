package com.example.quiz.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.quiz.model.Question;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Classe utilitaire pour gérer les médias (images, vidéos, sons)
 * en l'absence de Firebase Storage
 */
public class MediaUtils {
    private static final String TAG = "MediaUtils";
    private static final int MAX_IMAGE_DIMENSION = 800; // Taille max pour une image
    private static final int MAX_BASE64_SIZE = 1024 * 1024; // 1MB max pour stocker en base64 dans Firestore
    private static final String MEDIA_DIR = "quiz_media";

    /**
     * Convertit une image en chaîne Base64 pour stockage dans Firestore
     * (uniquement pour les petites images)
     */
    public static String imageToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        
        // Réduire la taille de l'image si nécessaire
        Bitmap resizedBitmap = resizeImageIfNeeded(bitmap);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        
        // Vérifier la taille
        if (imageBytes.length > MAX_BASE64_SIZE) {
            Log.w(TAG, "Image trop grande pour être stockée en Base64 dans Firestore: " + imageBytes.length + " bytes");
            return null;
        }
        
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
    
    /**
     * Convertit une chaîne Base64 en bitmap
     */
    public static Bitmap base64ToImage(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        
        byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
    
    /**
     * Redimensionne une image si elle dépasse les dimensions maximales
     */
    private static Bitmap resizeImageIfNeeded(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return bitmap;
        }
        
        float ratio = (float) width / height;
        int newWidth, newHeight;
        
        if (width > height) {
            newWidth = MAX_IMAGE_DIMENSION;
            newHeight = (int) (newWidth / ratio);
        } else {
            newHeight = MAX_IMAGE_DIMENSION;
            newWidth = (int) (newHeight * ratio);
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    
    /**
     * Sauvegarde un média dans le stockage local de l'application
     */
    public static String saveMediaToInternalStorage(Context context, Uri mediaUri) {
        try {
            // Créer un dossier pour les médias s'il n'existe pas
            File mediaDir = new File(context.getFilesDir(), MEDIA_DIR);
            if (!mediaDir.exists()) {
                mediaDir.mkdirs();
            }
            
            // Générer un nom de fichier unique
            String filename = UUID.randomUUID().toString();
            
            // Déterminer l'extension
            String mimeType = context.getContentResolver().getType(mediaUri);
            String extension = getExtensionFromMimeType(mimeType);
            String fullFilename = filename + extension;
            
            // Créer le fichier
            File outputFile = new File(mediaDir, fullFilename);
            
            // Copier les données
            try (InputStream is = context.getContentResolver().openInputStream(mediaUri);
                 FileOutputStream fos = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }
            
            return fullFilename;
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la sauvegarde du média: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obtient le chemin complet d'un média stocké localement
     */
    public static File getMediaFile(Context context, String filename) {
        if (filename == null || filename.isEmpty()) return null;
        
        File mediaDir = new File(context.getFilesDir(), MEDIA_DIR);
        return new File(mediaDir, filename);
    }
    
    /**
     * Détermine l'extension de fichier à partir du type MIME
     */
    private static String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return ".jpg"; // Par défaut
        
        switch (mimeType) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "video/mp4":
                return ".mp4";
            case "audio/mp3":
                return ".mp3";
            case "audio/mpeg":
                return ".mp3";
            default:
                return "." + mimeType.split("/")[1];
        }
    }
    
    /**
     * Met à jour les métadonnées d'un média dans Firestore
     * Cela permet de stocker uniquement des références aux médias, pas les médias eux-mêmes
     */
    public static void storeMediaReference(String questionId, String mediaFilename, String mediaType) {
        Map<String, Object> mediaData = new HashMap<>();
        mediaData.put("mediaFilename", mediaFilename);
        mediaData.put("mediaType", mediaType);
        
        FirebaseFirestore.getInstance()
            .collection("question_media")
            .document(questionId)
            .set(mediaData)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Référence média enregistrée"))
            .addOnFailureListener(e -> Log.e(TAG, "Erreur lors de l'enregistrement de la référence média", e));
    }
    
    /**
     * Récupère un média pour une question depuis Firestore et le stockage local
     */
    public static void loadQuestionMedia(@NonNull Context context, @NonNull Question question, @NonNull OnMediaLoadedListener listener) {
        String questionId = question.getId();
        if (questionId == null) {
            listener.onMediaLoadError("ID de question invalide");
            return;
        }
        
        // D'abord essayer de lire depuis Firestore
        FirebaseFirestore.getInstance()
            .collection("question_media")
            .document(questionId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String mediaFilename = documentSnapshot.getString("mediaFilename");
                    String mediaType = documentSnapshot.getString("mediaType");
                    
                    if (mediaFilename != null) {
                        // Charger depuis le stockage local
                        File mediaFile = getMediaFile(context, mediaFilename);
                        if (mediaFile != null && mediaFile.exists()) {
                            listener.onMediaLoaded(mediaFile.getAbsolutePath(), mediaType);
                        } else {
                            listener.onMediaLoadError("Fichier média non trouvé");
                        }
                    } else {
                        listener.onMediaLoadError("Nom de fichier média manquant");
                    }
                } else {
                    // Vérifier si les données média sont stockées directement dans la question
                    String imageUrl = question.getImageUrl();
                    String videoUrl = question.getVideoUrl();
                    String mediaType = null;
                    String base64Media = null;
                    
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        mediaType = "image";
                        base64Media = imageUrl;
                    } else if (videoUrl != null && !videoUrl.isEmpty()) {
                        mediaType = "video";
                        base64Media = videoUrl;
                    }
                    
                    if (base64Media != null && !base64Media.isEmpty()) {
                        listener.onBase64MediaLoaded(base64Media, mediaType);
                    } else {
                        listener.onMediaLoadError("Aucun média trouvé pour cette question");
                    }
                }
            })
            .addOnFailureListener(e -> listener.onMediaLoadError("Erreur: " + e.getMessage()));
    }
    
    /**
     * Interface de callback pour le chargement des médias
     */
    public interface OnMediaLoadedListener {
        void onMediaLoaded(String mediaPath, String mediaType);
        void onBase64MediaLoaded(String base64Media, String mediaType);
        void onMediaLoadError(String errorMessage);
    }

    /**
     * Charge une image depuis une URL ou un Base64 et l'affiche dans une ImageView
     */
    public static void loadImage(Context context, String imageSource, ImageView imageView) {
        if (context == null || imageSource == null || imageView == null) {
            return;
        }

        // Vérifier si l'image est un base64
        if (imageSource.startsWith("data:image") || imageSource.startsWith("data:image/")) {
            // Image en Base64
            loadBase64Image(context, imageSource, imageView);
        } else if (imageSource.startsWith("gs://")) {
            // Image dans Firebase Storage (non utilisé, mais pour compatibilité)
            Log.w(TAG, "Firebase Storage n'est pas utilisé dans cette application");
            // Afficher une image par défaut ou un message
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        } else if (imageSource.startsWith("http://") || imageSource.startsWith("https://")) {
            // Image depuis une URL
            loadUrlImage(context, imageSource, imageView);
        } else {
            // Considérer comme une URL par défaut
            loadUrlImage(context, imageSource, imageView);
        }
    }

    /**
     * Charge une image Base64 dans une ImageView
     */
    private static void loadBase64Image(Context context, String base64String, ImageView imageView) {
        try {
            // Extraire la partie Base64 de la chaîne (après la virgule)
            String[] parts = base64String.split(",");
            String base64Data = parts.length > 1 ? parts[1] : parts[0];

            // Convertir la chaîne Base64 en tableau d'octets
            byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);

            // Convertir le tableau d'octets en Bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            // Afficher l'image
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement de l'image Base64", e);
        }
    }

    /**
     * Charge une image depuis Firebase Storage
     * Note: Cette méthode est désactivée puisque Firebase Storage n'est pas utilisé
     */
    private static void loadFirebaseStorageImage(Context context, String gsUrl, ImageView imageView) {
        // Nous n'utilisons pas Firebase Storage
        Log.w(TAG, "Firebase Storage n'est pas utilisé dans cette application");
        // Afficher une image par défaut
        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
    }

    /**
     * Charge une image depuis une URL
     */
    private static void loadUrlImage(Context context, String url, ImageView imageView) {
        try {
            // Charger l'image avec Glide
            Glide.with(context)
                .load(url)
                .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement de l'image depuis l'URL", e);
        }
    }
} 