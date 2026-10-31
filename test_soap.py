"""Prueba manual de integración contra el endpoint SOAP de VetSphere.

Verifica la operación `consultarProducto` del servicio SOAP publicado por el
backend en http://localhost:8080/soap/productos, incluyendo la cabecera
WS-Security UsernameToken + PasswordDigest (SHA-1 sobre nonce + created + password).

CREDENCIALES POR VARIABLE DE ENTORNO (nunca se versionan):
    SOAP_USERNAME   usuario WS-Security (default: vetsphere-local)
    SOAP_PASSWORD   contraseña WS-Security (obligatoria si el endpoint está activo)
    SOAP_URL        URL del endpoint (default: http://localhost:8080/soap/productos)
    SOAP_CODIGO     código de producto a consultar (default: MED-001)

Uso:
    $env:SOAP_PASSWORD="tu-clave"; python test_soap.py
"""

import base64
import datetime
import hashlib
import os
import sys
import urllib.error
import urllib.request

SOAP_USER = os.environ.get("SOAP_USERNAME", "vetsphere-local")
SOAP_PASS = os.environ.get("SOAP_PASSWORD", "")
SOAP_URL = os.environ.get("SOAP_URL", "http://localhost:8080/soap/productos")
SOAP_CODIGO = os.environ.get("SOAP_CODIGO", "MED-001")

# Namespaces fijos por especificación (SOAP 1.1 y WS-Security siempre usan http://)
NS_ENVELOPE = "http://schemas.xmlsoap.org/soap/envelope/"
NS_WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
NS_WSU = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
NS_USERNAME_TOKEN = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0"
NS_SOAP_SECURITY = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0"
NS_PROD = "https://soap.vetsphere.com/productos"


def construir_digest(password: str) -> tuple[str, str, str]:
    """Genera el PasswordDigest de WS-Security: Base64(SHA-1(nonce + created + password))."""
    nonce = os.urandom(16)
    nonce_b64 = base64.b64encode(nonce).decode("utf-8")
    created = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.000Z")
    sha1 = hashlib.sha1()
    sha1.update(nonce)
    sha1.update(created.encode("utf-8"))
    sha1.update(password.encode("utf-8"))
    digest = base64.b64encode(sha1.digest()).decode("utf-8")
    return digest, nonce_b64, created


def consultar_producto(codigo: str) -> tuple[int, str]:
    """Invoca consultarProducto y devuelve (status, cuerpo)."""
    digest, nonce_b64, created = construir_digest(SOAP_PASS)
    envelope = f"""<soapenv:Envelope xmlns:soapenv="{NS_ENVELOPE}" xmlns:prod="{NS_PROD}">
   <soapenv:Header>
      <wsse:Security xmlns:wsse="{NS_WSSE}" xmlns:wsu="{NS_WSU}">
         <wsse:UsernameToken>
            <wsse:Username>{SOAP_USER}</wsse:Username>
            <wsse:Password Type="{NS_USERNAME_TOKEN}#PasswordDigest">{digest}</wsse:Password>
            <wsse:Nonce EncodingType="{NS_SOAP_SECURITY}#Base64Binary">{nonce_b64}</wsse:Nonce>
            <wsu:Created>{created}</wsu:Created>
         </wsse:UsernameToken>
      </wsse:Security>
   </soapenv:Header>
   <soapenv:Body>
      <prod:consultarProducto><codigo>{codigo}</codigo></prod:consultarProducto>
   </soapenv:Body>
</soapenv:Envelope>"""

    req = urllib.request.Request(
        SOAP_URL,
        data=envelope.encode("utf-8"),
        headers={"Content-Type": "text/xml;charset=UTF-8"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return resp.status, resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8")


def main() -> int:
    if not SOAP_PASS:
        print("[FALLO] Define la variable de entorno SOAP_PASSWORD antes de ejecutar.")
        return 1

    print(f"Consultando producto {SOAP_CODIGO!r} en {SOAP_URL} ...")
    status, cuerpo = consultar_producto(SOAP_CODIGO)

    print(f"Status HTTP: {status}")
    print(f"Respuesta:\n{cuerpo}")

    if status == 200 and "<soapenv:Fault>" not in cuerpo:
        print("[OK] El endpoint SOAP respondió correctamente con WS-Security.")
        return 0

    print("[FALLO] El servicio SOAP devolvió un fault o un error HTTP.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
