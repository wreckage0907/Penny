from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import user, stock, file
from services.stock_sim import simulate_stock_prices
from database.firebase import StockDatabase
from database.firebase_init import db
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
app.include_router(stock.router)
app.include_router(file.router)

@app.on_event("startup")
async def startup_event():
    stock_db = StockDatabase(db)
    asyncio.create_task(simulate_stock_prices(stock_db))