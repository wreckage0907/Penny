from pydantic import BaseModel

class Stock(BaseModel):
    symbol: str
    price: float
    timestamp: str