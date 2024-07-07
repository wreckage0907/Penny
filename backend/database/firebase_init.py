from firebase_admin import credentials, firestore, initialize_app, storage
import os

def initialize_firebase():
    current = os.path.dirname(__file__)
    path = os.path.join(current, "../penny-89979-firebase-adminsdk-spc33-5699359ee2.json")
    cred = credentials.Certificate(path)
    firebase_app = initialize_app(cred, {
        'storageBucket': 'penny-89979.appspot.com'
    })
    return firestore.client(), storage.bucket()

db, firebase_storage = initialize_firebase()