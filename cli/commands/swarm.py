import typer
from commands.common.swarm_command import *
from .adb import Device

app = typer.Typer()

@app.command()
def status(): # Get Swarm Status 0011010-1 is ON / 0000010-1 is OFF
    device = Device()
    sw_status = swarm_status(device)
    if sw_status == True:
        typer.echo("Swarm status: " + typer.style("Swarm is ON", fg=typer.colors.GREEN, bold=True))
    else:
        typer.echo("Swarm status: " + typer.style("Swarm is OFF", fg=typer.colors.RED, bold=True))

@app.command()
def id(): #  Get Swarm id 
    device = Device()
    sw_id = swarm_id(device) 
    typer.echo("Swarm device identifier: " + typer.style(sw_id, fg=typer.colors.GREEN, bold=True))

@app.command()
def gps(): # Get GPS Fix Quality (Example return $GS 109,214,9,0,G3*46)
    device = Device()
    sw_gps = swarm_gps(device) 
    typer.echo("Swarm gps: " + typer.style(sw_gps, fg=typer.colors.GREEN, bold=True))

@app.command()
def fw(): # Get firmware version of swarm (Example return 2021-07-16-00:10:21,v1.1.0*74)
    device = Device() 
    sw_fw = swarm_firmware(device) 
    typer.echo("Swarm firmware: " + typer.style(sw_fw, fg=typer.colors.GREEN, bold=True))

@app.command()
def dt(): # Get Datetime of swarm (Example return 20190408195123,V*41)
    device = Device()
    sw_dt = swarm_datetime(device) 
    typer.echo("Swarm datetime: " + typer.style(sw_dt, fg=typer.colors.GREEN, bold=True))



    