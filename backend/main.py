from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import user, stock, file

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
app.include_router(stock.router)
app.include_router(file.router)