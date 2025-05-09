import json
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import time
import argparse
import os

class FirestoreUploader:
    def __init__(self, credential_path):
        """
        Initialise l'uploader Firestore avec le chemin du fichier de credentials
        """
        self.cred = credentials.Certificate(credential_path)
        firebase_admin.initialize_app(self.cred)
        self.db = firestore.client()
        print("Connexion à Firestore établie avec succès")

    def upload_collection(self, collection_name, data):
        """
        Télécharge une collection entière de données
        """
        start_time = time.time()
        count = 0
        
        print(f"Début du téléchargement de la collection '{collection_name}'...")
        
        # Vérifie si les données sont une liste ou un dictionnaire
        if isinstance(data, list):
            # Si c'est une liste, chaque élément est un document avec un ID généré automatiquement
            batch = self.db.batch()
            batch_size = 0
            batch_limit = 500  # Limite de Firestore pour les opérations par lot
            
            for item in data:
                # Si l'élément a un ID, l'utiliser, sinon en générer un
                if 'id' in item:
                    doc_ref = self.db.collection(collection_name).document(item['id'])
                    # Supprimer l'ID du document avant de l'envoyer (optionnel)
                    doc_data = item.copy()
                    doc_data.pop('id', None)
                    batch.set(doc_ref, doc_data)
                else:
                    doc_ref = self.db.collection(collection_name).document()
                    batch.set(doc_ref, item)
                
                batch_size += 1
                count += 1
                
                # Si nous atteignons la limite du lot, le soumettre et en créer un nouveau
                if batch_size >= batch_limit:
                    batch.commit()
                    print(f"Lot de {batch_size} documents téléchargé")
                    batch = self.db.batch()
                    batch_size = 0
            
            # Soumettre le dernier lot s'il contient des documents
            if batch_size > 0:
                batch.commit()
                print(f"Dernier lot de {batch_size} documents téléchargé")
                
        elif isinstance(data, dict):
            # Si c'est un dictionnaire, chaque clé est l'ID d'un document
            batch = self.db.batch()
            batch_size = 0
            batch_limit = 500
            
            for doc_id, doc_data in data.items():
                doc_ref = self.db.collection(collection_name).document(doc_id)
                batch.set(doc_ref, doc_data)
                
                batch_size += 1
                count += 1
                
                if batch_size >= batch_limit:
                    batch.commit()
                    print(f"Lot de {batch_size} documents téléchargé")
                    batch = self.db.batch()
                    batch_size = 0
            
            # Soumettre le dernier lot
            if batch_size > 0:
                batch.commit()
                print(f"Dernier lot de {batch_size} documents téléchargé")
        
        end_time = time.time()
        duration = end_time - start_time
        
        print(f"Téléchargement terminé: {count} documents ajoutés à '{collection_name}' en {duration:.2f} secondes")
        return count

    def upload_json_file(self, json_file_path):
        """
        Télécharge toutes les collections depuis un fichier JSON
        """
        try:
            with open(json_file_path, 'r', encoding='utf-8') as file:
                data = json.load(file)
            
            total_docs = 0
            start_time = time.time()
            
            # Parcourir chaque collection dans le fichier JSON
            for collection_name, collection_data in data.items():
                docs_count = self.upload_collection(collection_name, collection_data)
                total_docs += docs_count
            
            end_time = time.time()
            total_duration = end_time - start_time
            
            print(f"\nRésumé du téléchargement:")
            print(f"Total des documents téléchargés: {total_docs}")
            print(f"Temps total: {total_duration:.2f} secondes")
            print(f"Vitesse moyenne: {total_docs/total_duration:.2f} documents/seconde")
            
            return True
        except Exception as e:
            print(f"Erreur lors du téléchargement du fichier JSON: {str(e)}")
            return False

def main():
    file_path = "quiz_data_fixed.json"
    creds_path = "serviceAccountKey.json"
    parser = argparse.ArgumentParser(description='Télécharger des données JSON vers Firebase Firestore')
    parser.add_argument('json_file', help='Chemin vers le fichier JSON contenant les données')
    
    # Vérifier si les fichiers existent
    if not os.path.exists(file_path):
        print(f"Erreur: Le fichier JSON '{file_path}' n'existe pas")
        return
    
    if not os.path.exists(creds_path):
        print(f"Erreur: Le fichier de credentials '{creds_path}' n'existe pas")
        return
    
    # Créer l'uploader et télécharger les données
    uploader = FirestoreUploader(creds_path)
    success = uploader.upload_json_file(file_path)
    
    if success:
        print("Téléchargement réussi!")
    else:
        print("Le téléchargement a échoué.")

if __name__ == "__main__":
    main()
