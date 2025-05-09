import json
import re

def fix_json_file(input_file, output_file):
    print(f"Lecture du fichier {input_file}...")
    
    # Lire le contenu du fichier
    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Nettoyer le contenu
    # Supprimer les ellipses qui causent des problèmes
    content = re.sub(r'\.\.\.\s*', '', content)
    
    # Extraire toutes les questions
    print("Extraction des objets questions...")
    questions_pattern = r'{\s*"id"\s*:\s*"[^"]+",\s*"authorId"\s*:\s*"system".*?"type"\s*:\s*"SINGLE_CHOICE".*?}'
    questions = re.findall(questions_pattern, content, re.DOTALL)
    
    # Créer une structure pour stocker toutes les questions
    all_questions = []
    
    # Traiter chaque question trouvée
    print(f"Traitement de {len(questions)} questions trouvées...")
    for q_str in questions:
        try:
            # Essayer de parser la question en JSON
            question = json.loads(q_str)
            all_questions.append(question)
            print(f"Question traitée avec succès: {question['id']}")
        except json.JSONDecodeError as e:
            print(f"Erreur lors du décodage d'une question: {str(e)}")
            # On pourrait ajouter ici une logique pour tenter de réparer les questions mal formées
    
    # Extraire les thèmes
    print("Recherche des thèmes dans le fichier...")
    themes_pattern = r'{\s*"id"\s*:\s*"theme_[^"]+",\s*"authorId"\s*:\s*"system".*?"updatedAt"\s*:\s*\d+\s*}'
    themes = re.findall(themes_pattern, content, re.DOTALL)
    
    all_themes = []
    
    # Traiter chaque thème trouvé
    print(f"Traitement de {len(themes)} thèmes trouvés...")
    for t_str in themes:
        try:
            theme = json.loads(t_str)
            all_themes.append(theme)
            print(f"Thème traité avec succès: {theme['id']}")
        except json.JSONDecodeError as e:
            print(f"Erreur lors du décodage d'un thème: {str(e)}")
    
    # Créer la structure finale
    final_json = {
        "questions": all_questions,
        "themes": all_themes
    }
    
    # Écrire le JSON corrigé dans le fichier de sortie
    print(f"Écriture du JSON corrigé dans {output_file}...")
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(final_json, f, ensure_ascii=False, indent=2)
    
    print(f"Correction terminée! {len(all_questions)} questions et {len(all_themes)} thèmes ont été sauvegardés.")
    return len(all_questions), len(all_themes)

# Si des thèmes ne sont pas trouvés, vous pouvez les ajouter manuellement
def add_missing_themes(output_file, themes_data):
    try:
        # Lire le fichier JSON existant
        with open(output_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # Ajouter les thèmes manquants
        data['themes'].extend(themes_data)
        
        # Écrire le fichier mis à jour
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        print(f"{len(themes_data)} thèmes supplémentaires ajoutés.")
    except Exception as e:
        print(f"Erreur lors de l'ajout des thèmes manquants: {str(e)}")

# Utilisation du script
if __name__ == "__main__":
    input_file = "injection.json"  # Remplacez par le chemin de votre fichier
    output_file = "quiz_data_fixed.json"
    
    questions_count, themes_count = fix_json_file(input_file, output_file)
    
    # Si vous avez besoin d'ajouter des thèmes manquants
    if themes_count == 0:
        print("Aucun thème trouvé. Ajout de thèmes par défaut...")
        default_themes = [
            {
                "id": "theme_histoire",
                "authorId": "system",
                "authorName": "Quiz Système",
                "category": "",
                "createdAt": 1743405450786,
                "description": "Testez vos connaissances sur les grands événements qui ont façonné notre monde.",
                "difficulty": "Moyen",
                "imageUrl": None,
                "isPublic": True,
                "playCount": 145,
                "published": True,
                "questionIds": [f"hist{i}" for i in range(1, 21)],
                "rating": 4.7,
                "timeLimit": 0,
                "title": "Histoire du monde",
                "updatedAt": 1743405450786
            }
            # Ajoutez d'autres thèmes au besoin
        ]
        add_missing_themes(output_file, default_themes)
    
    print(f"Le fichier JSON a été corrigé et enregistré sous {output_file}")
