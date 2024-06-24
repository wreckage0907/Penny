from firebase_admin import credentials, firestore, initialize_app
import os

def initialize_firebase():
    current = os.path.dirname(__file__)
    path = os.path.join(current, "../firebase.json")
    cred = credentials.Certificate(path)
    initialize_app(cred)
    return firestore.client()

db = initialize_firebase()