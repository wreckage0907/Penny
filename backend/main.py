from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import user, stock
from services.stock_sim import simulate_stock_prices
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

@app.on_event("startup")
async def startup_event():
    asyncio.create_task(simulate_stock_prices(stock.stock_db))