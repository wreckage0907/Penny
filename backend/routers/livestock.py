from fastapi import APIRouter, Query, HTTPException
from pydantic import BaseModel
from datetime import datetime, timedelta
from fastapi.responses import JSONResponse
import yfinance as yf
from database.firebase_init import firestore_db as db

router = APIRouter()

class BuySellRequest(BaseModel):
    user_id: str
    ticker: str
    quantity: int

def get_historical_data(ticker, start_date, end_date):
    stock = yf.Ticker(ticker)
    historical_data = stock.history(start=start_date, end=end_date)
    return historical_data

def get_live_data(ticker):
    stock = yf.Ticker(ticker)
    live_data = stock.history(period="1d", interval="1m")
    return live_data

def get_current_price(ticker):
    stock = yf.Ticker(ticker)
    price = stock.history(period="1d")["Close"].iloc[-1]
    return price

def get_user_portfolio(user_id):
    doc_ref = db.collection(user_id).document('user')
    doc = doc_ref.get()
    if doc.exists:
        return doc.to_dict()['user']
    else:
        return None

def update_user_portfolio(user_id, portfolio):
    doc_ref = db.collection(user_id).document('user')
    doc_ref.update(portfolio)

@router.post("/buy")
async def buy_stock(request: BuySellRequest):
    user_id = request.user_id
    ticker = request.ticker
    quantity = request.quantity

    portfolio = get_user_portfolio(user_id)
    if portfolio is None:
        raise HTTPException(status_code=404, detail="User not found")
    
    price = get_current_price(ticker)
    total_cost = price * quantity
    
    if portfolio['coins'] < total_cost:
        raise HTTPException(status_code=400, detail="Insufficient coins")
    
    portfolio['coins'] -= total_cost
    holdings = portfolio.get('holdings', {})
    holdings[ticker] = holdings.get(ticker, 0) + quantity
    portfolio['holdings'] = holdings
    transactions = portfolio.get('transactions', [])
    transactions.append({
        'type': 'buy',
        'ticker': ticker,
        'quantity': quantity,
        'price': price,
        'total_cost': total_cost,
        'date': datetime.now().isoformat()
    })
    portfolio['transactions'] = transactions

    update_user_portfolio(user_id, portfolio)
    
    return {"message": "Stock bought successfully", "portfolio": portfolio}

@router.post("/sell")
async def sell_stock(request: BuySellRequest):
    user_id = request.user_id
    ticker = request.ticker
    quantity = request.quantity
    
    portfolio = get_user_portfolio(user_id)
    if portfolio is None:
        raise HTTPException(status_code=404, detail="User not found")
    
    holdings = portfolio.get('holdings', {})
    if ticker not in holdings or holdings[ticker] < quantity:
        raise HTTPException(status_code=400, detail="Insufficient shares")
    
    price = get_current_price(ticker)
    total_gain = price * quantity
    
    portfolio['coins'] += total_gain
    holdings[ticker] -= quantity
    if holdings[ticker] == 0:
        del holdings[ticker]
    portfolio['holdings'] = holdings
    transactions = portfolio.get('transactions', [])
    transactions.append({
        'type': 'sell',
        'ticker': ticker,
        'quantity': quantity,
        'price': price,
        'total_gain': total_gain,
        'date': datetime.now().isoformat()
    })
    portfolio['transactions'] = transactions
    
    update_user_portfolio(user_id, portfolio)
    
    return {"message": "Stock sold successfully", "portfolio": portfolio}

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
