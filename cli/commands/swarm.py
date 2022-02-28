import typer
from .adb import Device

app = typer.Typer()


@app.command()
def id():
    device = Device()
    swarm_id = device.shell('echo 0x00ab12')
    typer.echo("Swarm device identifier: " + typer.style(swarm_id, fg=typer.colors.GREEN, bold=True))

