"""
Verificacion de integridad y calidad del sistema VetSphere.

Comprueba la estructura minima de los tres servicios (backend, frontend y
cliente SOAP), la presencia de los controles de seguridad, del asistente de
IA, de las pruebas automatizadas y de la documentacion tecnica. Sirve como
verificacion rapida en CI y como smoke test local.

Uso:
    python test_calidad_sistema.py
"""

import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent
BACKEND = RAIZ / "Backend-Proyecto-Lenguajes-main" / "Backend-Proyecto-Lenguajes-main"
FRONTEND = RAIZ / "Frontend-Proyecto-Lenguajes-main" / "Frontend-Proyecto-Lenguajes-main"
MINI = RAIZ / "MiniProyecto-SOAP-master" / "MiniProyecto-SOAP-master"


def probar_estructura_proyecto():
    for d in (BACKEND.parent, FRONTEND.parent, MINI.parent):
        assert d.exists() and d.is_dir(), f"Directorio requerido no encontrado: {d.name}"
    for d in (BACKEND, FRONTEND, MINI):
        assert d.is_dir(), f"Servicio anidado no encontrado: {d.name}"


def probar_backend_configuracion():
    pom = BACKEND / "pom.xml"
    assert pom.exists(), "pom.xml de backend no encontrado"
    contenido = pom.read_text(encoding="utf-8")
    assert "spring-boot" in contenido, "Dependencia Spring Boot no detectada"
    assert "vetsphere-backend" in contenido, "Artefacto de backend no detectado"
    assert "spring-boot-starter-test" in contenido, "Dependencia de pruebas ausente (spring-boot-starter-test)"


def probar_backend_seguridad():
    seguridad = BACKEND / "src" / "main" / "java" / "com" / "VetSphere" / "Backend" / "config" / "SecurityConfig.java"
    assert seguridad.exists(), "SecurityConfig.java no encontrado"
    contenido = seguridad.read_text(encoding="utf-8")
    assert "permitAll" in contenido and "hasAnyRole" in contenido, "Reglas de autorizacion por rol no detectadas"
    jwt = seguridad.parent.parent / "security" / "JwtService.java"
    assert jwt.exists(), "JwtService.java no encontrado"


def probar_backend_asistente_ia():
    controladores = BACKEND / "src" / "main" / "java" / "com" / "VetSphere" / "Backend" / "modules" / "clinic" / "controller"
    encontrados = [f for f in controladores.glob("*.java") if "chat" in f.name.lower()]
    assert encontrados, "Controlador del asistente de IA (ChatController) no encontrado"


def probar_backend_pruebas():
    java_tests = list((BACKEND / "src" / "test").rglob("*.java"))
    assert java_tests, "No hay clases de prueba Java en src/test/java"
    assert len(java_tests) >= 5, f"Numero de clases de prueba insuficiente ({len(java_tests)})"


def probar_frontend_configuracion():
    pkg = FRONTEND / "package.json"
    assert pkg.exists(), "package.json de frontend no encontrado"
    datos = json.loads(pkg.read_text(encoding="utf-8"))
    assert "dependencies" in datos, "Seccion dependencies ausente en frontend"
    angular = [d for d in datos["dependencies"] if d.startswith("@angular/core")]
    assert angular, "Dependencia Angular core no detectada"
    assert "lucide-angular" not in datos["dependencies"], "Dependencia sin uso lucide-angular debería eliminarse"


def probar_frontend_entornos():
    env = FRONTEND / "src" / "environments" / "environment.ts"
    env_prod = FRONTEND / "src" / "environments" / "environment.prod.ts"
    assert env.exists(), "environment.ts no encontrado (URL del API hardcodeada)"
    assert env_prod.exists(), "environment.prod.ts no encontrado"
    angular_json = json.loads((FRONTEND / "angular.json").read_text(encoding="utf-8"))
    assert "fileReplacements" in json.dumps(angular_json), "fileReplacements de entornos no registrado en angular.json"


def probar_frontend_asistente_ia():
    chatbot = FRONTEND / "src" / "app" / "components" / "chatbot" / "chatbot.ts"
    assert chatbot.exists(), "Componente del asistente de IA (chatbot) no encontrado"


def probar_frontend_pruebas():
    specs = list((FRONTEND / "src").rglob("*.spec.ts"))
    assert len(specs) >= 10, f"Numero de pruebas frontend insuficiente ({len(specs)})"
    core_specs = [s for s in specs if "core" in s.parts]
    assert core_specs, "No hay pruebas de los servicios core (interceptor, guard, autorizacion)"


def probar_soap_servicio():
    soap_pom = MINI / "pom.xml"
    assert soap_pom.exists(), "pom.xml de MiniProyecto SOAP no encontrado"
    contenido = soap_pom.read_text(encoding="utf-8")
    assert "mini-soap-client" in contenido, "Artefacto mini soap no detectado"
    xsd = BACKEND / "src" / "main" / "resources" / "soap" / "productos.xsd"
    assert xsd.exists(), "Esquema SOAP productos.xsd no encontrado"


def probar_documentacion_tecnica():
    requeridos = [
        RAIZ / "README.md",
        RAIZ / "CHANGELOG.md",
        RAIZ / "docs" / "INSTALACION.md",
        RAIZ / "docs" / "ARQUITECTURA.md",
        RAIZ / ".github" / "workflows" / "ci.yml",
        BACKEND / "README.md",
        FRONTEND / "README.md",
    ]
    for f in requeridos:
        assert f.exists(), f"Documento de documentacion ausente: {f.relative_to(RAIZ)}"


def probar_secretos_versionados():
    entorno = BACKEND / ".env"
    if entorno.exists():
        gitignore = BACKEND / ".gitignore"
        assert gitignore.exists() and ".env" in gitignore.read_text(encoding="utf-8"), \
            ".env existe localmente pero no esta excluido en .gitignore (riesgo de versionar secretos)"
    test_soap = (RAIZ / "test_soap.py").read_text(encoding="utf-8")
    assert "os.environ.get" in test_soap, "test_soap.py debe leer credenciales del entorno"
    assert "P45VK5Xek2bteoGgkCLSAs" not in test_soap, "Credencial hardcodeada detectada en test_soap.py"


PRUEBAS = [
    ("Estructura de arquitectura", probar_estructura_proyecto),
    ("Integridad Backend Spring Boot", probar_backend_configuracion),
    ("Seguridad del Backend (roles + JWT)", probar_backend_seguridad),
    ("Asistente de IA del Backend", probar_backend_asistente_ia),
    ("Pruebas automatizadas del Backend", probar_backend_pruebas),
    ("Integridad Frontend Angular", probar_frontend_configuracion),
    ("Entornos de Angular", probar_frontend_entornos),
    ("Asistente de IA del Frontend", probar_frontend_asistente_ia),
    ("Pruebas automatizadas del Frontend", probar_frontend_pruebas),
    ("Integridad Servicio SOAP (XSD + mini cliente)", probar_soap_servicio),
    ("Documentacion tecnica", probar_documentacion_tecnica),
    ("Ausencia de secretos versionados", probar_secretos_versionados),
]


def main() -> int:
    todas_exitosas = True
    for nombre, fn in PRUEBAS:
        try:
            fn()
            print(f"[OK] {nombre}")
        except AssertionError as err:
            print(f"[FALLO] {nombre}: {err}")
            todas_exitosas = False

    if not todas_exitosas:
        print("\nVerificacion de calidad: HAY FALLOS.")
        return 1

    print(f"\nVerificacion de calidad: {len(PRUEBAS)} pruebas superadas.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
