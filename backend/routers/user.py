from fastapi import APIRouter
from firebase_admin import credentials, firestore, initialize_app

router = APIRouter()

cred = credentials.ApplicationDefault()
initialize_app(cred)
db = firestore.client()

@router.get("/user")
def get_user(user_id: str):
    user_ref = db.collection("users").document(user_id)
    user = user_ref.get().to_dict()
    return user


@router.post("/user")
def create_user(user_id: str, user: dict):
    user_ref = db.collection("users").document(user_id)
    user_ref.set(user)
    return user
