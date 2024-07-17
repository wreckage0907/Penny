from firebase_admin import credentials, firestore, initialize_app, storage
import os
import json

def initialize_firebase():
    cred = credentials.Certificate("/app/firebase.json")
    try:
        firebase_app = initialize_app(cred, {
            'storageBucket': 'penny-b0b59.appspot.com'
        })
        return firestore.client(), storage.bucket()
    except Exception as e:
        print(f"Error initializing Firebase: {str(e)}")
        return None, None

firestore_db, firebase_storage = initialize_firebase()