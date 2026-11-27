import logging
import os
import shutil
import threading
import time
import urllib.parse

from selenium import webdriver
from selenium.common.exceptions import NoSuchElementException, TimeoutException
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait

logger = logging.getLogger("whatsapp_client")


def _resolver_chrome_bin() -> str:
    env_bin = os.getenv("CHROME_BIN", "").strip()
    if env_bin and os.path.exists(env_bin):
        return env_bin
    for candidato in (
        "/usr/bin/chromium",
        "/usr/bin/chromium-browser",
        "/usr/bin/google-chrome",
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    ):
        if os.path.exists(candidato):
            return candidato
    encontrado = (
        shutil.which("chromium")
        or shutil.which("chromium-browser")
        or shutil.which("google-chrome")
    )
    return encontrado or env_bin


CHROME_BIN = _resolver_chrome_bin()
CHROMEDRIVER_BIN = os.getenv("CHROMEDRIVER_BIN", "")

WHATSAPP_URL = "https://web.whatsapp.com"
QR_CANVAS_SELECTOR = "canvas[aria-label='Scan this QR code to link a device!'], div[data-testid='qrcode'] canvas"
CHAT_LIST_SELECTOR = "div[aria-label='Chat list'], div[id='pane-side']"
MESSAGE_BOX_SELECTOR = "div[contenteditable='true'][data-tab='10'], footer div[contenteditable='true']"
SEND_BUTTON_SELECTOR = "button[aria-label='Send'], span[data-icon='send']"
INVALID_NUMBER_TEXT = "Phone number shared via url is invalid"
PHONE_NUMBER_SHORTCUT_SELECTOR = "[data-testid='link_device_qr_phone_number_shortcut_link']"
PHONE_NUMBER_INPUT_SELECTOR = "[data-testid='phone-number-input']"
LINK_CODE_CELLS_SELECTOR = "[data-testid='link-with-phone-number-code-cells']"
COUNTRY_SELECTOR_BUTTON = "[data-testid='phone-number-country-selector']"
COUNTRY_SEARCH_INPUT = "[data-testid='search-input']"
COUNTRY_FIRST_RESULT = "[data-testid='list-item-0']"


class WhatsAppWebClient:
    """
    Envoltorio sobre Selenium para automatizar WhatsApp Web sin usar la API
    oficial de Meta. Requiere vincular manualmente el dispositivo una vez
    (escaneando el QR o codigo de 8 caracteres) y mantiene la sesion en un
    perfil de Chrome persistente.
    """

    def __init__(self, profile_dir: str, qr_path: str, headless: bool = True):
        self.profile_dir = os.path.abspath(profile_dir)
        self.qr_path = os.path.abspath(qr_path)
        self.headless = headless
        self.driver = None
        self.logged_in = False
        self._lock = threading.Lock()

    def iniciar(self):
        options = Options()
        if CHROME_BIN and os.path.exists(CHROME_BIN):
            options.binary_location = CHROME_BIN
        options.add_argument(f"--user-data-dir={self.profile_dir}")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--disable-gpu")
        options.add_argument("--no-zygote")
        options.add_argument("--disable-extensions")
        options.add_argument("--disable-background-networking")
        options.add_argument("--disable-default-apps")
        options.add_argument("--mute-audio")
        options.add_argument("--js-flags=--max-old-space-size=192")
        options.add_argument("--window-size=1024,768")
        options.add_argument("--lang=es-CO")
        options.add_argument("--disable-blink-features=AutomationControlled")
        options.add_experimental_option("excludeSwitches", ["enable-automation"])
        options.add_experimental_option("useAutomationExtension", False)
        options.add_argument(
            "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        )
        if self.headless:
            options.add_argument("--headless=new")

        if CHROMEDRIVER_BIN and os.path.isfile(CHROMEDRIVER_BIN):
            service = Service(CHROMEDRIVER_BIN)
            self.driver = webdriver.Chrome(service=service, options=options)
        else:
            self.driver = webdriver.Chrome(options=options)

        self.driver.execute_cdp_cmd(
            "Page.addScriptToEvaluateOnNewDocument",
            {"source": "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"},
        )
        self.driver.execute_cdp_cmd("Emulation.setFocusEmulationEnabled", {"enabled": True})
        self.driver.get(WHATSAPP_URL)
        logger.info("WhatsApp Web abierto, esperando sesion o QR...")
        self._esperar_sesion_o_qr()

    def reiniciar_sesion(self):
        with self._lock:
            if self.driver is not None:
                try:
                    self.driver.quit()
                except Exception:
                    logger.exception("Error cerrando el driver anterior")
            self.driver = None
            self.logged_in = False
            shutil.rmtree(self.profile_dir, ignore_errors=True)
            os.makedirs(self.profile_dir, exist_ok=True)
            if os.path.exists(self.qr_path):
                os.remove(self.qr_path)

        logger.info("Sesion reiniciada, arrancando de nuevo desde cero...")
        self.iniciar()

    def _esperar_sesion_o_qr(self, qr_stale_segundos: int = 45):
        ultimo_qr_bytes = None
        ultimo_cambio = time.time()
        while True:
            try:
                with self._lock:
                    if self._chat_list_visible():
                        self.logged_in = True
                        logger.info("Sesion de WhatsApp Web activa.")
                        return
                    hay_qr = self._guardar_qr_si_existe()
                if hay_qr:
                    with open(self.qr_path, "rb") as f:
                        actual = f.read()
                    if actual != ultimo_qr_bytes:
                        ultimo_qr_bytes = actual
                        ultimo_cambio = time.time()
                    elif time.time() - ultimo_cambio > qr_stale_segundos:
                        logger.info("QR estancado por %ss, recargando pagina...", qr_stale_segundos)
                        with self._lock:
                            self.driver.refresh()
                        ultimo_qr_bytes = None
                        ultimo_cambio = time.time()
                        time.sleep(3)
                        continue
                    logger.info("QR disponible en %s, esperando escaneo...", self.qr_path)
            except Exception:
                logger.exception("Error en el loop de espera de sesion/QR, reintentando...")
            time.sleep(2)

    def _chat_list_visible(self) -> bool:
        try:
            self.driver.find_element(By.CSS_SELECTOR, CHAT_LIST_SELECTOR)
            return True
        except NoSuchElementException:
            return False

    def _guardar_qr_si_existe(self) -> bool:
        try:
            qr = self.driver.find_element(By.CSS_SELECTOR, QR_CANVAS_SELECTOR)
            qr.screenshot(self.qr_path)
            return True
        except NoSuchElementException:
            return False

    def estado(self) -> dict:
        if self.driver is None:
            return {"logged_in": False, "iniciado": False}
        with self._lock:
            logged_in = self._chat_list_visible()
        self.logged_in = logged_in
        return {"logged_in": logged_in, "iniciado": True}

    def enviar_mensaje(self, telefono: str, mensaje: str) -> dict:
        if self.driver is None:
            raise RuntimeError("El cliente de WhatsApp Web no ha sido iniciado")

        with self._lock:
            return self._enviar_mensaje_sin_lock(telefono, mensaje)

    def _enviar_mensaje_sin_lock(self, telefono: str, mensaje: str) -> dict:
        texto = urllib.parse.quote(mensaje)
        url = f"{WHATSAPP_URL}/send?phone={telefono}&text={texto}"
        self.driver.get(url)

        wait = WebDriverWait(self.driver, 30)
        try:
            wait.until(lambda d: self._numero_invalido() or self._caja_mensaje_lista())
        except TimeoutException:
            return {"success": False, "error": "Timeout esperando que cargara el chat"}

        if self._numero_invalido():
            return {"success": False, "error": "El numero no tiene WhatsApp o es invalido"}

        try:
            boton = self.driver.find_element(By.CSS_SELECTOR, SEND_BUTTON_SELECTOR)
            boton.click()
        except NoSuchElementException:
            caja = self.driver.find_element(By.CSS_SELECTOR, MESSAGE_BOX_SELECTOR)
            caja.send_keys(Keys.ENTER)

        time.sleep(2)
        return {"success": True}

    def _numero_invalido(self) -> bool:
        return INVALID_NUMBER_TEXT in self.driver.page_source

    def _caja_mensaje_lista(self) -> bool:
        try:
            self.driver.find_element(By.CSS_SELECTOR, MESSAGE_BOX_SELECTOR)
            return True
        except NoSuchElementException:
            return False

    def solicitar_codigo(self, telefono: str, pais: str = "Colombia", timeout_segundos: int = 20) -> str:
        if self.driver is None:
            raise RuntimeError("El cliente de WhatsApp Web no ha sido iniciado")

        with self._lock:
            if self._chat_list_visible():
                raise RuntimeError("Ya hay una sesion activa, no se necesita codigo")

            wait = WebDriverWait(self.driver, timeout_segundos)

            try:
                atajo = wait.until(
                    lambda d: d.find_element(By.CSS_SELECTOR, PHONE_NUMBER_SHORTCUT_SELECTOR)
                )
                self._click(atajo)
            except TimeoutException:
                logger.info("Atajo de 'vincular con numero' no visible, se asume que ya se paso esa pantalla.")

            wait.until(
                lambda d: (d.find_element(By.CSS_SELECTOR, PHONE_NUMBER_INPUT_SELECTOR).get_attribute("value") or "").strip().startswith("+")
            )

            selector_pais = wait.until(
                lambda d: d.find_element(By.CSS_SELECTOR, COUNTRY_SELECTOR_BUTTON)
            )
            self._click(selector_pais)

            busqueda = wait.until(lambda d: d.find_element(By.CSS_SELECTOR, COUNTRY_SEARCH_INPUT))
            busqueda.send_keys(pais)

            def _resultado_filtrado(d):
                el = d.find_element(By.CSS_SELECTOR, COUNTRY_FIRST_RESULT)
                return el if pais.lower() in el.text.lower() else False

            primer_resultado = wait.until(_resultado_filtrado)
            self._click(primer_resultado)

            campo = wait.until(lambda d: d.find_element(By.CSS_SELECTOR, PHONE_NUMBER_INPUT_SELECTOR))
            campo.send_keys(telefono)

            boton = self.driver.find_element(
                By.XPATH,
                "//button[contains(., 'Next') or contains(., 'Siguiente')]"
            )
            self._click(boton)

            celdas = wait.until(lambda d: d.find_element(By.CSS_SELECTOR, LINK_CODE_CELLS_SELECTOR))
            crudo = celdas.get_attribute("data-link-code")
            caracteres = crudo.split(",")
            codigo = "".join(caracteres)
            return f"{codigo[:4]}-{codigo[4:]}"

    def _click(self, elemento):
        try:
            elemento.click()
        except Exception:
            self.driver.execute_script("arguments[0].click();", elemento)