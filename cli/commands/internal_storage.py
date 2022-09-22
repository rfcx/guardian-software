from ctypes.wintypes import PINT
from commands.common.internal_storage_command import *
import typer
from .adb import Device

app = typer.Typer()

@app.command()
def get():
    device = Device()
    storage = getStorage(device)
    typer.echo(storage)

@app.command()
def remove():
    device = Device()
    storage = removeStorage(device)
    typer.echo(storage)
