import asyncio
from datetime import datetime
from schema.stock import Stock
from database.firebase import StockDatabase

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