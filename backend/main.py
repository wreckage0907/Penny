from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import user, file, expenseTracker, chatbot, onboarding, questionGen
from database.firebase_init import firestore_db
import asyncio

app = FastAPI()
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
    allow_credentials=True
)

@app.get("/")
async def root():
    return {"message": "Hello World"}

app.include_router(user.router)
app.include_router(file.router)
app.include_router(expenseTracker.router)
app.include_router(chatbot.router)
app.include_router(onboarding.router)
app.include_router(questionGen.router)
