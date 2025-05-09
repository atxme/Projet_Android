import json
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import time
import os

class QuizFirestoreUploader:
    def __init__(self, credential_path):
        """
        Initialise la connexion à Firestore avec les identifiants fournis
        """
        self.cred = credentials.Certificate(credential_path)
        firebase_admin.initialize_app(self.cred)
        self.db = firestore.client()
        print("Connexion à Firestore établie avec succès")

    def upload_questions(self, questions_data):
        """
        Télécharge les questions dans la collection 'questions'
        """
        batch_size = 0
        batch_limit = 450  # Limite de Firestore pour les opérations par lot
        batch = self.db.batch()
        count = 0
        
        print("Début du téléchargement des questions...")
        
        for question in questions_data:
            # Utiliser l'ID fourni dans le JSON
            question_id = question.get('id')
            if question_id:
                doc_ref = self.db.collection('questions').document(question_id)
                
                # Créer une copie pour éviter de modifier les données originales
                question_data = question.copy()
                
                # Supprimer l'ID du document car il est déjà utilisé comme clé
                if 'id' in question_data:
                    del question_data['id']
                
                batch.set(doc_ref, question_data)
                batch_size += 1
                count += 1
                
                # Si nous atteignons la limite du lot, le soumettre et en créer un nouveau
                if batch_size >= batch_limit:
                    batch.commit()
                    print(f"Lot de {batch_size} questions téléchargé")
                    batch = self.db.batch()
                    batch_size = 0
                    time.sleep(1)  # Pause pour éviter les limitations de taux
        
        # Soumettre le dernier lot s'il contient des documents
        if batch_size > 0:
            batch.commit()
            print(f"Dernier lot de {batch_size} questions téléchargé")
        
        print(f"Téléchargement terminé: {count} questions ajoutées")
        return count

    def upload_themes(self, themes_data):
        """
        Télécharge les thèmes dans la collection 'quizzes'
        """
        batch_size = 0
        batch_limit = 450
        batch = self.db.batch()
        count = 0
        
        print("Début du téléchargement des thèmes...")
        
        for theme in themes_data:
            theme_id = theme.get('id')
            if theme_id:
                doc_ref = self.db.collection('quizzes').document(theme_id)
                
                theme_data = theme.copy()
                if 'id' in theme_data:
                    del theme_data['id']
                
                batch.set(doc_ref, theme_data)
                batch_size += 1
                count += 1
                
                if batch_size >= batch_limit:
                    batch.commit()
                    print(f"Lot de {batch_size} thèmes téléchargé")
                    batch = self.db.batch()
                    batch_size = 0
                    time.sleep(1)
        
        if batch_size > 0:
            batch.commit()
            print(f"Dernier lot de {batch_size} thèmes téléchargé")
        
        print(f"Téléchargement terminé: {count} thèmes ajoutés")
        return count

    def upload_quiz_data(self, json_file_path):
        """
        Charge les données de quiz à partir d'un fichier JSON et les télécharge dans Firestore
        """
        try:
            with open(json_file_path, 'r', encoding='utf-8') as file:
                data = json.load(file)
            
            total_questions = 0
            total_themes = 0
            
            # Télécharger les questions
            if 'questions' in data:
                total_questions = self.upload_questions(data['questions'])
            
            # Télécharger les thèmes
            if 'themes' in data:
                total_themes = self.upload_themes(data['themes'])
            
            print(f"\nRésumé du téléchargement:")
            print(f"Questions téléchargées: {total_questions}")
            print(f"Thèmes téléchargés: {total_themes}")
            
            return True
        except Exception as e:
            print(f"Erreur lors du téléchargement: {str(e)}")
            return False

    def check_existing_data(self):
        """
        Vérifie les données existantes dans Firestore
        """
        questions_ref = self.db.collection('questions')
        quizzes_ref = self.db.collection('quizzes')
        
        questions_count = len(list(questions_ref.stream()))
        quizzes_count = len(list(quizzes_ref.stream()))
        
        print(f"Données existantes dans Firestore:")
        print(f"- Questions: {questions_count}")
        print(f"- Quiz: {quizzes_count}")
        
        return questions_count, quizzes_count

def main():
    # Chemin vers le fichier de clé de service Firebase
    credential_path = "serviceAccountKey.json"
    
    # Chemin vers le fichier JSON contenant les données de quiz
    json_file_path = "quiz_data_fixed.json"
    
    # Vérifier si les fichiers existent
    if not os.path.exists(credential_path):
        print(f"Erreur: Le fichier de credentials '{credential_path}' n'existe pas")
        return
    
    if not os.path.exists(json_file_path):
        print(f"Erreur: Le fichier JSON '{json_file_path}' n'existe pas")
        return
    
    # Créer l'uploader et télécharger les données
    uploader = QuizFirestoreUploader(credential_path)
    
    # Vérifier les données existantes
    uploader.check_existing_data()
    
    # Demander confirmation avant de continuer
    response = input("Voulez-vous continuer avec le téléchargement ? (o/n): ")
    if response.lower() != 'o':
        print("Téléchargement annulé.")
        return
    
    # Télécharger les données
    success = uploader.upload_quiz_data(json_file_path)
    
    if success:
        print("Téléchargement réussi!")
    else:
        print("Le téléchargement a échoué.")

if __name__ == "__main__":
    main()
    