from ctypes.wintypes import PINT
import typer
import ctypes
from .adb import Device

app = typer.Typer()


@app.command()
def status(): # TODO Get i2c status 
    device = Device()
    result = execute(device, '0x4a') 
    i2c_status = result[0:6] 
    if i2c_status == '0x0001':
        typer.echo("i2c status: " + typer.style(i2c_status+" (ON)", fg=typer.colors.GREEN, bold=True))
    elif i2c_status == '0x0000' :
        typer.echo("i2c status: " + typer.style(i2c_status+" (OFF)", fg=typer.colors.RED, bold=True))
    else:
        typer.echo("i2c status: " + typer.style("read fail", fg=typer.colors.RED, bold=True))

@app.command()
def input_info(): # TODO Get input information (Voltage, Currect)
    device = Device()

    result = execute(device, '0x3b') 
    input_v = result[0:6]
    input_v = int(input_v, 16)*1.648
    input_v = float("{:.2f}".format(input_v))
    typer.echo("Input voltage: " + typer.style(input_v, fg=typer.colors.GREEN, bold=True))

    result = execute(device, '0x3e') 
    input_c = result[0:6]
    input_c = int(input_c, 16)*(0.00146487 /0.005)
    input_c = float("{:.2f}".format(input_c))
    typer.echo("Input Current: " + typer.style(input_c, fg=typer.colors.GREEN, bold=True))

@app.command()
def system_info(): # TODO Get system information (Voltage)
    device = Device()

    result = execute(device, '0x3c') 
    sys_v = result[0:6]
    sys_v = int(sys_v, 16)*1.648
    sys_v = float("{:.2f}".format(sys_v))
    typer.echo("System voltage: " + typer.style(sys_v, fg=typer.colors.GREEN, bold=True))

@app.command()
def bat_info(): # TODO Get battery information (Voltage, Current, battery percentage)
    device = Device()

    result = execute(device, '0x3a') 
    bat_v = result[0:6]
    bat_v = int(bat_v, 16)*0.192264
    bat_v = float("{:.2f}".format(bat_v))
    typer.echo("Battery voltage: " + typer.style(bat_v, fg=typer.colors.GREEN, bold=True))

    result = execute(device, '0x3d') 
    bat_c = result[0:6]
    bat_c = int(bat_c, 16)
    bat_c = ctypes.c_int16(bat_c).value
    bat_c = bat_c*(0.00146487/0.003)
    bat_c = float("{:.2f}".format(bat_c))
    typer.echo("Battery Current: " + typer.style(bat_c, fg=typer.colors.GREEN, bold=True))

    result = execute(device, '0x13') 
    bat_p = result[0:6]
    bat_p =((int(bat_p, 16)-16384)/32768)*100
    bat_p = float("{:.2f}".format(bat_p))
    typer.echo("Battery percentage: " + typer.style(str(bat_p)+' %', fg=typer.colors.GREEN, bold=True))

def execute(device, command):
    shell_command = f'i2cget -y 1 0x68 {command} w'
    typer.echo(shell_command)
    response = device.shell(shell_command)
    # TODO unwrap $ and * from response
    return response