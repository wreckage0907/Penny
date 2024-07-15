from firebase_admin import credentials, firestore, initialize_app, storage
import os
import json

def initialize_firebase():
    current = os.path.dirname(__file__)
    firebase_env = os.getenv('firebase') 
    if firebase_env:
        path = json.loads(firebase_env)
    else:
        path = os.path.join(current, "../firebase.json")
        
    cred = credentials.Certificate(path)
    firebase_app = initialize_app(cred, {
        'storageBucket': 'penny-89979.appspot.com'
    })
    return firestore.client(), storage.bucket()

firestore_db, firebase_storage = initialize_firebase()
