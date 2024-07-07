from firebase_admin import credentials, firestore, initialize_app
import os

def initialize_firebase_firestore():
    current = os.path.dirname(__file__)
    path = os.path.join(current, "../firebase.json")
    cred = credentials.Certificate(path)
    firebase_app = initialize_app(cred)
    return firestore.client()

firestore_db = initialize_firebase_firestore()
