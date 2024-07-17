from firebase_admin import credentials, firestore, initialize_app, storage
import os
import json

def initialize_firebase():
    # Check for FIREBASE_JSON environment variable first (for Render)
    firebase_json = os.getenv('FIREBASE_JSON')
    if firebase_json:
        try:
            cred = credentials.Certificate(json.loads(firebase_json))
        except json.JSONDecodeError:
            print("Error: FIREBASE_JSON environment variable is not valid JSON")
            return None, None
    else:
        # Fall back to file-based approach
        current = os.path.dirname(__file__)
        
        # Check for 'firebase' environment variable
        firebase_env = os.getenv('firebase') 
        if firebase_env:
            try:
                path = json.loads(firebase_env)
            except json.JSONDecodeError:
                print("Error: 'firebase' environment variable is not valid JSON")
                return None, None
        elif os.path.exists('/etc/secrets/firebase.json'):
            path = '/etc/secrets/firebase.json'
        else:
            path = os.path.join(current, "../firebase-backend.json")
        
        try:
            cred = credentials.Certificate(path)
        except Exception as e:
            print(f"Error loading Firebase credentials: {str(e)}")
            return None, None

    try:
        firebase_app = initialize_app(cred, {
            'storageBucket': 'penny-b0b59.appspot.com'
        })
        return firestore.client(), storage.bucket()
    except Exception as e:
        print(f"Error initializing Firebase: {str(e)}")
        return None, None

firestore_db, firebase_storage = initialize_firebase()