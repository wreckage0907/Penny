from fastapi import APIRouter, Query
import yfinance as yf
import pandas as pd
from datetime import datetime, timedelta
from fastapi.responses import JSONResponse
from schema.livestock import StockRequest, HistoricalRequest

router = APIRouter()

def get_historical_data(ticker, start_date, end_date):
    stock = yf.Ticker(ticker)
    historical_data = stock.history(start=start_date, end=end_date)
    return historical_data

def get_live_data(ticker):
    stock = yf.Ticker(ticker)
    live_data = stock.history(period="1d", interval="1m")
    return live_data


@router.get("/historical")
async def historical(
    ticker: str = Query(..., description="Stock ticker symbol"),
    start_date: str = Query(None, description="Start date in YYYY-MM-DD format"),
    end_date: str = Query(None, description="End date in YYYY-MM-DD format")
):
    if start_date is None:
        end_date = datetime.now()
        start_date = (end_date - timedelta(days=1)).strftime('%Y-%m-%d')
    else:
        end_date = end_date or datetime.now().strftime('%Y-%m-%d')
    
    start_date = datetime.strptime(start_date, '%Y-%m-%d')
    end_date = datetime.strptime(end_date, '%Y-%m-%d')

    data = get_historical_data(ticker, start_date, end_date)
    data.index = data.index.strftime('%Y-%m-%d %H:%M:%S')  
    return JSONResponse(content=data.to_dict(orient='index'))

@router.get("/live")
async def live(ticker: str = Query(..., description="Stock ticker symbol")):
    data = get_live_data(ticker)
    data.index = data.index.strftime('%Y-%m-%d %H:%M:%S')  
    return JSONResponse(content=data.to_dict(orient='index'))