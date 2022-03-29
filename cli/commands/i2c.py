from ctypes.wintypes import PINT
from commands.common.i2c_command import *
import typer
from .adb import Device

app = typer.Typer()

@app.command()
def status(): #  Get i2c status 
    device = Device()
    i2c_status = i2cStatus(device)
    if i2c_status == True:
        typer.echo("i2c status: " + typer.style("i2c is ON", fg=typer.colors.GREEN, bold=True))
    else :
        typer.echo("i2c status: " + typer.style("i2c is OFF", fg=typer.colors.RED, bold=True))

@app.command()
def input_info(): # Get input information (Voltage, Currect)
    device = Device()
    input_v = input_voltage(device)
    typer.echo("Input voltage: " + typer.style("{:.2f}".format(input_v), fg=typer.colors.GREEN, bold=True))

    input_c = input_current(device)
    input_c = float("{:.2f}".format(input_c))
    typer.echo("Input Current: " + typer.style(input_c, fg=typer.colors.GREEN, bold=True))

@app.command()
def system_info(): # Get system information (Voltage)
    device = Device()
    sys_v = system_voltage(device)
    sys_v = float("{:.2f}".format(sys_v))
    typer.echo("System voltage: " + typer.style(sys_v, fg=typer.colors.GREEN, bold=True))

@app.command()
def bat_info(): # Get battery information (Voltage, Current, battery percentage)
    device = Device()
    bat_v = battery_voltage(device)
    bat_v = float("{:.2f}".format(bat_v))
    typer.echo("Battery voltage: " + typer.style(bat_v, fg=typer.colors.GREEN, bold=True))

    bat_c = battery_cuurent(device)
    bat_c = float("{:.2f}".format(bat_c))
    typer.echo("Battery Current: " + typer.style(bat_c, fg=typer.colors.GREEN, bold=True))

    bat_p = battery_percentage(device)
    bat_p = float("{:.2f}".format(bat_p))
    typer.echo("Battery percentage: " + typer.style(str(bat_p)+' %', fg=typer.colors.GREEN, bold=True))

