import typer
from .adb import Device

app = typer.Typer()


@app.command()
def id():
    device = Device()
    result = execute(device, '$CS*10') # TODO replace with CS (and add $ + *10 automatically)
    swarm_id = result # TODO parse the result
    typer.echo("Swarm device identifier: " + typer.style(swarm_id, fg=typer.colors.GREEN, bold=True))

def execute(device, command):
    shell_command = f'echo -n \'{command}\\r\' > /dev/ttyMT1 | /system/xbin/busybox timeout 1 /system/bin/cat < /dev/ttyMT1'
    typer.echo(shell_command)
    response = device.shell(shell_command)
    # TODO unwrap $ and * from response
    return response
