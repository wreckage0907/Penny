from fastapi import APIRouter, HTTPException
from typing import List
from schema.stock import Stock 
from database.firebase import StockDatabase
from database.firebase_init import firestore_db

router = APIRouter()
stock_firestore_db = StockDatabase(firestore_db)

@router.get("/stocks", response_model=List[Stock])
async def get_stocks():
    return stock_firestore_db.get_all_stocks()

@router.get("/stock/{symbol}", response_model=Stock)
async def get_stock(symbol: str):
    stock = stock_firestore_db.get_stock(symbol)
    if stock:
        return stock
    raise HTTPException(status_code=404, detail="Stock not found")