from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import user

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
