"""
logger.py – Structured logging for Kinetix PC Server.

Logs to both console (coloured) and ``logs/server.log`` with rotation
(5 MB per file, 3 backups).
"""

import logging
import os
import sys
from logging.handlers import RotatingFileHandler

_LOG_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "logs")
_LOG_FILE = os.path.join(_LOG_DIR, "server.log")
_MAX_BYTES = 5 * 1024 * 1024  # 5 MB
_BACKUP_COUNT = 3
_FMT = "%(asctime)s  %(levelname)-8s  %(name)-18s  %(message)s"
_DATE_FMT = "%Y-%m-%d %H:%M:%S"

_initialised = False


def setup_logging(level: int = logging.DEBUG) -> None:
    """Initialise the root logger with console + file handlers."""
    global _initialised
    if _initialised:
        return
    _initialised = True

    os.makedirs(_LOG_DIR, exist_ok=True)

    formatter = logging.Formatter(_FMT, datefmt=_DATE_FMT)

    # File handler (rotating)
    fh = RotatingFileHandler(
        _LOG_FILE, maxBytes=_MAX_BYTES, backupCount=_BACKUP_COUNT, encoding="utf-8"
    )
    fh.setLevel(logging.DEBUG)
    fh.setFormatter(formatter)

    # Console handler
    ch = logging.StreamHandler(sys.stdout)
    ch.setLevel(level)
    ch.setFormatter(formatter)

    root = logging.getLogger()
    root.setLevel(logging.DEBUG)
    root.addHandler(fh)
    root.addHandler(ch)


def get_logger(name: str) -> logging.Logger:
    """Return a named child logger (auto-initialises if needed)."""
    setup_logging()
    return logging.getLogger(name)
