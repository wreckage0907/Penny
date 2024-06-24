from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from pydantic import BaseModel
from typing import List
import firebase_admin
from firebase_admin import credentials, firestore
import asyncio
from datetime import datetime

# Data Models
class Stock(BaseModel):
    symbol: str
    price: float
    timestamp: str

# Firebase Setup
def setup_firebase():
    if not firebase_admin._apps:
        cred = credentials.Certificate("firebase-credentials.json")
        firebase_admin.initialize_app(cred)
    return firestore.client()

# FastAPI Setup
def create_app():
    app = FastAPI()
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    return app

# Database Operations
class StockDatabase:
    def __init__(self, db):
        self.db = db

    def update_stock(self, stock: Stock):
        self.db.collection('stocks').document(stock.symbol).set(stock.dict())

    def get_all_stocks(self):
        stocks = self.db.collection('stocks').get()
        return [Stock(**stock.to_dict()) for stock in stocks]

    def get_stock(self, symbol: str):
        stock = self.db.collection('stocks').document(symbol).get()
        if stock.exists:
            return Stock(**stock.to_dict())
        return None

# Stock Price Simulation
async def simulate_stock_prices(db: StockDatabase):
    while True:
        stocks = [
            Stock(symbol="AAPL", price=150.0, timestamp=datetime.now().isoformat()),
            Stock(symbol="GOOGL", price=2500.0, timestamp=datetime.now().isoformat()),
            Stock(symbol="MSFT", price=300.0, timestamp=datetime.now().isoformat())
        ]
        for stock in stocks:
            db.update_stock(stock)
        await asyncio.sleep(60)  # Update every minute

# API Routes
def setup_routes(app: FastAPI, db: StockDatabase):
    @app.get("/stocks", response_model=List[Stock])
    async def get_stocks():
        return db.get_all_stocks()

    @app.get("/stock/{symbol}", response_model=Stock)
    async def get_stock(symbol: str):
        stock = db.get_stock(symbol)
        if stock:
            return stock
        raise HTTPException(status_code=404, detail="Stock not found")

# Main Application
app = create_app()
db = setup_firebase()
stock_db = StockDatabase(db)

@app.on_event("startup")
async def startup_event():
    asyncio.create_task(simulate_stock_prices(stock_db))

@app.on_event("shutdown")
def shutdown_event():
    if firebase_admin._apps:
        firebase_admin.delete_app(firebase_admin.get_app())

setup_routes(app, stock_db)

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True) 

