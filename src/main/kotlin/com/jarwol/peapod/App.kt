package com.jarwol.peapod

import com.mailjet.client.ClientOptions
import com.mailjet.client.MailjetClient
import com.mailjet.client.MailjetRequest
import com.mailjet.client.resource.Emailv31
import org.json.JSONArray
import org.json.JSONObject
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

val logger = LoggerFactory.getLogger("peapod")

fun main() {
    val options = FirefoxOptions().setHeadless(true)
    val driver = FirefoxDriver(options)
    driver.get("https://www.peapod.com")

    try {
        waitUntilPageIsReady(driver)
        login(driver)
        waitUntilPageIsReady(driver)
        closeModal(driver)
        val (reserved, msg) = reserveTimeSlot(driver)
        logger.info(msg)

        if (reserved) {
            sendAlert("Peapod Scraper", msg)
        }
    } catch (e: Exception) {
        logger.error(e.message, e)
    } finally {
        driver.quit()
    }
}

fun waitUntilPageIsReady(driver: RemoteWebDriver) {
    val executor = driver as JavascriptExecutor
    WebDriverWait(driver, 5)
        .until { executor.executeScript("return document.readyState") == "complete" }
}

fun login(driver: RemoteWebDriver) {
    driver.findElementByClassName("gateway-header_auth-sign-in").click()
    driver.findElementById("username").sendKeys("jared@jarwol.com"")
    driver.findElementById("password").sendKeys("blahblah")
    driver.findElementByCssSelector("form.login-form").submit()
}

fun closeModal(driver: RemoteWebDriver) {
    try {
        WebDriverWait(driver, 10)
            .until(ExpectedConditions.elementToBeClickable(By.className("optly-modal-close")))

        val modalClose = driver.findElementsByClassName("optly-modal-close").firstOrNull()
        modalClose?.click()
    } catch (e: TimeoutException) {
        logger.warn("Couldn't find modal to close. Trying to continue anyway.")
    }

}

fun reserveTimeSlot(driver: RemoteWebDriver): Pair<Boolean, String> {
    driver.findElementByLinkText("Reserve a Time").click()
    return if (reserveTimeSlot(driver, LocalDate.now().plusDays(2))) {
        Pair(
            true, driver
                .findElementByLinkText("Change")
                .findElement(By.xpath("./preceding-sibling::span[1]"))
                .text
        )
    } else {
        Pair(false, "No time slot available")
    }
}

fun reserveTimeSlot(driver: RemoteWebDriver, date: LocalDate): Boolean {
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.US)
    val day = date.dayOfMonth

    logger.info("Checking time slots for ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}")

    WebDriverWait(driver, 5)
        .until(ExpectedConditions.presenceOfElementLocated(By.className("slot-selection-container")))

    val boxDay =
        driver.findElementsByXPath("""//div[@class="box_month"][text()="$month"]/following-sibling::div[@class="box_date"][text()="$day"]""")
            .firstOrNull()

    return if (boxDay == null) {
        false
    } else {
        val ariaLabel = "Delivery Times for ${date.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd"))}"
        driver.findElementByXPath("""//li[@class="time-slot" and contains(@aria-label, "$ariaLabel")]""").click()
        WebDriverWait(driver, 5)
            .until(ExpectedConditions.presenceOfElementLocated(By.id("time-slot-day-header")))
        val btn = driver.findElementsByCssSelector("li.slot:not(.sold-out) button").firstOrNull()
        if (btn != null) {
            btn.click()
            true
        } else {
            reserveTimeSlot(driver, date.plusDays(1))
        }
    }
}

fun sendAlert(subject: String, body: String) {
    val client =
        MailjetClient("fakekey", "fakesecret", ClientOptions("v3.1"))

    val request = MailjetRequest(Emailv31.resource)
        .property(
            Emailv31.MESSAGES, JSONArray()
                .put(
                    JSONObject()
                        .put(
                            Emailv31.Message.FROM, JSONObject()
                                .put("Email", "jared@jarwol.com")
                                .put("Name", "Jared")
                        )
                        .put(
                            Emailv31.Message.TO, JSONArray()
                                .put(
                                    JSONObject()
                                        .put("Email", "jared@jarwol.com")
                                        .put("Name", "Jared Wolinsky")
                                )
                        )
                        .put(Emailv31.Message.SUBJECT, subject)
                        .put(Emailv31.Message.TEXTPART, body)
                        .put(Emailv31.Message.HTMLPART, body)
                        .put(Emailv31.Message.CUSTOMID, "PeapodScraper")
                )
        );
    val response = client.post(request);
    logger.info("Send email status: ${response.status}")
}
