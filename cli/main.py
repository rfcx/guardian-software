import typer
from commands.swarm import app as swarm

app = typer.Typer()
app.add_typer(swarm, name="swarm")

if __name__ == "__main__":
    app()
