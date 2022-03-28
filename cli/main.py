import typer
from commands.swarm import app as swarm
from commands.i2c import app as i2c
from commands.ui import load as ui_load

app = typer.Typer()
app.add_typer(swarm, name="swarm")
app.add_typer(i2c, name="i2c")

@app.command()
def ui():
    ui_load()

if __name__ == "__main__":
    app()
