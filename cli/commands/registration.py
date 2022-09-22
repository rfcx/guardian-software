from ctypes.wintypes import PINT
from commands.common.registration_command import *
import typer
from .adb import Device

app = typer.Typer()

@app.command()
def is_registered():
    device = Device()
    registration = isRegistered(device)
    typer.echo(registration)

@app.command()
def remove_registration():
    device = Device()
    registration = removeRegistration(device)
    typer.echo(registration)
