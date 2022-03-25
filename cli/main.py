import typer
from commands.swarm import app as swarm
from commands.i2c import app as i2c

app = typer.Typer()
app.add_typer(swarm, name="swarm")
app.add_typer(i2c, name="i2c")

if __name__ == "__main__":
    app()
