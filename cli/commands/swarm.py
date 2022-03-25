import typer
from .adb import Device

app = typer.Typer()


@app.command()
def status(): # TODO Get Swarm Status 0011010-1 is ON / 0000010-1 is OFF
    device = Device()
    shell_command = f'cat /sys/devices/virtual/misc/mtgpio/pin | grep \'128:\''
    sw_status = device.shell(shell_command)
    sw_status = sw_status.split("\n")
    sw_status = sw_status[0].split(":")
    sw_status = sw_status[1].split("\r")

    if sw_status[0] == "0011010-1":
        typer.echo("Swarm status: " + typer.style(sw_status[0]+" (ON)", fg=typer.colors.GREEN, bold=True))
    else:
        typer.echo("Swarm status: " + typer.style(sw_status[0]+" (OFF)", fg=typer.colors.RED, bold=True))

@app.command()
def id(): # TODO Get Swarm id 
    device = Device()
    result = execute(device, '$CS*10') # TODO replace with CS (and add $ + *10 automatically)
    swarm_id = result # TODO parse the result
    typer.echo("Swarm device identifier: " + typer.style(swarm_id, fg=typer.colors.GREEN, bold=True))

@app.command()
def gps(): # TODO Get GPS Fix Quality (Example return $GS 109,214,9,0,G3*46)
    device = Device()
    result = execute(device, '$GS @*74') 
    swarm_gps = result 
    typer.echo("Swarm gps: " + typer.style(swarm_gps, fg=typer.colors.GREEN, bold=True))

@app.command()
def fw(): # TODO Get firmware version of swarm (Example return 2021-07-16-00:10:21,v1.1.0*74)
    device = Device()
    result = execute(device, '$FV*10') 
    swarm_fw = result 
    typer.echo("Swarm firmware: " + typer.style(swarm_fw, fg=typer.colors.GREEN, bold=True))

@app.command()
def dt(): # TODO Get Datetime of swarm (Example return 20190408195123,V*41)
    device = Device()
    result = execute(device, '$DT @*70') 
    swarm_dt = result 
    typer.echo("Swarm firmware: " + typer.style(swarm_dt, fg=typer.colors.GREEN, bold=True))

def execute(device, command):
    shell_command = f'echo -n \'{command}\\r\' > /dev/ttyMT1 | /system/xbin/busybox timeout 1 /system/bin/cat < /dev/ttyMT1'
    typer.echo(shell_command)
    response = device.shell(shell_command)
    # TODO unwrap $ and * from response
    return response

    