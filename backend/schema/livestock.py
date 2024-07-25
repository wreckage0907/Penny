from pydantic import BaseModel
from datetime import datetime

class StockRequest(BaseModel):
    ticker: str

class HistoricalRequest(BaseModel):
    ticker: str
    start_date: datetime
    end_date: datetime