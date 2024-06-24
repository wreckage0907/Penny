from fastapi import APIRouter
from database.firebase_init import db

router = APIRouter()

@router.get("/user")
def get_user(user_id: str):
    user_ref = db.collection("users").document(user_id)
    user = user_ref.get().to_dict()
    return user