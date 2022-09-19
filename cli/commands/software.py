from ctypes.wintypes import PINT
from commands.common.software_command import *
import typer
from .adb import Device

app = typer.Typer()

@app.command()
def get():
    device = Device()
    softwares = getSoftwares(device)
    typer.echo(softwares)

@app.command()
def install(role: str):
    device = Device()
    softwares = downloadSoftwares(device, role)
    typer.echo(softwares)

@app.command()
def downgrade(role: str):
    device = Device()
    softwares = downgradeSoftwares(device, role)
    typer.echo(softwares) 
