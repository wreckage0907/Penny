from schema.stock import Stock  
from .firebase_init import firestore_db

class StockDatabase:
    def __init__(self, firestore_db):
        self.firestore_db = firestore_db

    def update_stock(self, stock: Stock):
        self.firestore_db.collection('stocks').document(stock.symbol).set(stock.dict())

    def get_all_stocks(self):
        stocks = self.firestore_db.collection('stocks').get()
        return [Stock(**stock.to_dict()) for stock in stocks]

    def get_stock(self, symbol: str):
        stock = self.firestore_db.collection('stocks').document(symbol).get()
        if stock.exists:
            return Stock(**stock.to_dict())
        return None