#!/usr/bin/env python3
"""Interactive lab bootstrap for a preinstalled, Device Owner MDMesh agent."""
import argparse
import getpass
from pathlib import Path
import re
import shlex
import shutil
import subprocess
from urllib.parse import urlsplit


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", help="ADB serial when multiple devices are connected")
    parser.add_argument("--helper", type=Path,
                        default=Path(__file__).with_name("MDMesh-enrolamiento-adb.apk"))
    args = parser.parse_args()
    if not shutil.which("adb"):
        parser.error("Instalá adb antes de ejecutar este script.")
    if not args.helper.is_file():
        parser.error("Falta el APK auxiliar; colocá MDMesh-enrolamiento-adb.apk junto al script o usá --helper.")
    inventory = subprocess.run(["adb", "devices"], capture_output=True, text=True)
    if inventory.returncode:
        parser.error("ADB no pudo listar los dispositivos.")
    ready = [line.split()[0] for line in inventory.stdout.splitlines()[1:]
             if len(line.split()) >= 2 and line.split()[1] == "device"]
    serial = args.serial or (ready[0] if len(ready) == 1 else None)
    if serial not in ready:
        parser.error("Conectá y autorizá una tablet; si hay varias, elegí una con --serial.")
    adb = ["adb", "-s", serial]
    installed = subprocess.run(adb + ["install", "-r", str(args.helper)])
    if installed.returncode:
        return installed.returncode
    server = input("URL HTTPS de la consola MDMesh: ").strip()
    try:
        uri = urlsplit(server)
        valid = (uri.scheme == "https" and bool(uri.hostname) and not uri.username
                 and not uri.password and not uri.query and not uri.fragment
                 and uri.path in ("", "/"))
    except ValueError:
        valid = False
    if not valid:
        parser.error("Usá el origen HTTPS de la consola, sin /rest, credenciales ni parámetros.")
    token = getpass.getpass("Token nuevo de Enroll → Token (entrada oculta): ").strip()
    if not token:
        parser.error("El token está vacío.")
    remote = shlex.join([
        "am", "instrument", "-w", "-e", "class",
        "com.mdmesh.agent.provisioning.AdbEnrollmentBootstrap",
        "-e", "serverUrl", server, "-e", "enrollToken", token,
        "com.mdmesh.agent.debug.test/androidx.test.runner.AndroidJUnitRunner",
    ])
    print("Esperando las credenciales del servidor (hasta 2 minutos)…", flush=True)
    result = subprocess.run(adb + ["shell", remote], capture_output=True, text=True)
    # Never print the command: it contains the enrollment token.
    output = result.stdout + result.stderr
    output = output.replace(token, "[token oculto]")
    print(output, end="" if output.endswith("\n") else "\n")
    if result.returncode or not re.search(r"\bOK \(1 test\)", output):
        print("No se confirmó el enrolamiento. Revisá el resultado y CheckInWorker antes de reintentar.")
        return result.returncode or 1
    print("Credenciales guardadas. Abrí MDM Agent y comprobá el dispositivo en la consola.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
