from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
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

@app.get("/", response_class=HTMLResponse)
async def root():
    html_content = """
    <html>
        <head>
            <title>Penny API</title>
        </head>
        <body style="display: flex; justify-content: center; align-items: flex-start; height: 100vh; margin: 0;">
            <h1>Penny API</h1>
        </body>
    </html>
    """
    return HTMLResponse(content=html_content)

app.include_router(user.router)
app.include_router(file.router)
app.include_router(expenseTracker.router)
app.include_router(chatbot.router)
app.include_router(onboarding.router)
app.include_router(questionGen.router)
