from pydantic import BaseModel

class Question(BaseModel):
    question: str
    options: list[str]
    ans: str
    hint: str

CHAPTERS = {
    "introduction_to_personal_finance": "Introduction to Personal Finance",
    "setting_financial_goals": "Setting Financial Goals",
    "budgeting_and_expense_tracking": "Budgeting and Expense Tracking",
    "online_and_mobile_banking": "Online and Mobile Banking"
}