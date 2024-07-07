from fastapi import APIRouter, HTTPException
from database.firebase_init import db

router = APIRouter()

@router.get("/chatbot/{user_id}")
def get_chat_history(user_id: str):
    chat_ref = db.collection("chatbot").document(user_id)
    chats = chat_ref.get()
    if chats.exists:
        return chats.to_dict()
    else:
        raise HTTPException(status_code=404, detail="User not found")
    
@router.post("/chatbot/{user_id}/add")
def add_chat_history(user_id: str, chat: dict):
    chat_ref = db.collection("chatbot").document(user_id)
    chat_ref.set(chat)
    return chat

@router.put("/chatbot/{user_id}/update")
def update_chat_history(user_id: str, chat: dict):
    chat_ref = db.collection("chatbot").document(user_id)
    chat_ref.update(chat)
    return chat

@router.delete("/chatbot/{user_id}/delete")
def delete_chat_history(user_id: str):
    chat_ref = db.collection("chatbot").document(user_id)
    chat_ref.delete()
    return {"message": "Chat history deleted"}