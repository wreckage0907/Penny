from fastapi import APIRouter
from firebase_admin import credentials, firestore, initialize_app
import os


router = APIRouter()

current = os.path.dirname(__file__)
path = os.path.join(current, "../firebase.json")
cred = credentials.Certificate(path)
initialize_app(cred)
db = firestore.client()

@router.get("/user")
def get_user(user_id: str):
    user_ref = db.collection("users").document(user_id)
    user = user_ref.get().to_dict()
    return user




