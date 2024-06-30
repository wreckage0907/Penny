import asyncio
from datetime import datetime
import logging
from schema.stock import Stock  
from database.firebase import StockDatabase

async def simulate_stock_prices(db: StockDatabase):
    while True:
        try:
            stocks = [
                Stock(symbol="AAPL", price=150.0, timestamp=datetime.now().isoformat()),
                Stock(symbol="GOOGL", price=2500.0, timestamp=datetime.now().isoformat()),
                Stock(symbol="MSFT", price=300.0, timestamp=datetime.now().isoformat())
            ]
            for stock in stocks:
                try:
                    db.update_stock(stock)
                except Exception as e:
                    logging.error(f"Error updating stock {stock.symbol}: {str(e)}")
            await asyncio.sleep(60)  
        except Exception as e:
            logging.error(f"Error in stock simulation: {str(e)}")
            await asyncio.sleep(60)  