import os
from pathlib import Path

_test_db = Path(__file__).resolve().parent / "clinica_test.db"
if _test_db.exists():
    _test_db.unlink()
os.environ["DATABASE_URL"] = f"sqlite:///{_test_db}"
