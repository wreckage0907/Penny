from pydantic import BaseModel
from typing import List, Dict, Optional

class SubCategory(BaseModel):
    name: Optional[str] = None

class Category(BaseModel):
    category: str
    total_budget: float = 0
    sub_categories: List[SubCategory] = []

class MonthData(BaseModel):
    month: str
    monthly_budget: float = 0
    amount_spent: float = 0
    categories: List[Category] = []

class YearData(BaseModel):
    year: str
    months: List[MonthData] = []

class Expense(BaseModel):
    years: Dict[str, List[MonthData]]

class User(BaseModel):
    first_name: str
    last_name: str
    email: str
    phone: str
