from fastapi import APIRouter, HTTPException
from database.firebase_init import firestore_db

router = APIRouter()

@router.get("/user")
def get_user(user_id: str):
    user_ref = firestore_db.collection(user_id).document("user")
    user = user_ref.get()
    if user.exists:
        return user.to_dict()
    raise HTTPException(status_code=404, detail="User not found")




