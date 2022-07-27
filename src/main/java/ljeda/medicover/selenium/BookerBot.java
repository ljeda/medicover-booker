package ljeda.medicover.selenium;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;
import ljeda.medicover.base.BotException;
import ljeda.medicover.log.Logger;

public class BookerBot {
	private static final String MEDICOVER_ONLINE_BASE_URL = "https://mol.medicover.pl";
	private static final String MAKE_APPOINTMENT_H3_TAG_TEXT = "Wizyta w Centrum Medicover";
	private static final String MAKE_APPOINTMENT_LINK_TEXT = "Umów wizytę";
	private static final String SEARCH_AVAILABLE_APPOINTMENTS_BUTTON_TEXT = "Szukaj";
	private static final String SHOW_MORE_BUTTON_TEXT = "Pokaż więcej ...";
	private static final int SLEEP_STEP = 100;
	private static final By DIV_BLOCKING_FORM = By.xpath("//div[@class='blockUI blockOverlay']");
	private static final By DIV_BLOCKING_SEARCH_RESULTS = By.xpath("//div[@class='ajax-loader']");
	private static final Locale LOCALE = new Locale("pl");

	private String username;
	private String password;
	private static int timeout = 5;
	private int maxRetries = 80;
	
	private final WebDriver driver;
	private final WebDriverWait wait;
	
	public BookerBot(String username, String password) {
		this.username = username;
		this.password = password;
		driver = setupFirefoxDriver();
		wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
	}

	public static void main(String[] args) {
		try {
			String username = "1228800";
			String password = "Eljot12.08";
			String specialty = "Dermatolog";
			String issue = "Umów poradę";
			
			new BookerBot(username, password).book(specialty, issue);
		} catch (NoSuchElementException e) {
			errorExit(new BotException("Expected element was not found on the web page", BotException.ErrorCodes.DOM_CHANGED));
		} catch (InterruptedException e) {
			errorExit(new BotException("Execution was interrupted by user", BotException.ErrorCodes.TIMEOUT));
		} catch (BotException e) {
			errorExit(e);
		}
	}
	
	private void book(String specialty, String issue) throws InterruptedException, BotException {
		login();
		displayAvailableSlots(specialty, issue);
		
		List<WebElement> h3NodesList = driver.findElements(By.xpath("//h3[@class='visitListDate']"));
		List<Slot> slots = new ArrayList<Slot>(h3NodesList.size());
		for (WebElement node : h3NodesList) {
			String dateString = node.getText();
			LocalDate date;
			try {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, EEEE", LOCALE);
				date = LocalDate.parse(dateString.toLowerCase(LOCALE), formatter);
			} catch (DateTimeParseException e) {
				throw new BotException(String.format("Unable to parse date: [%s]", dateString), BotException.ErrorCodes.UNEXPECTED_SITUATION);
			}
			List<WebElement> appSlots = node.findElements(By.xpath("./../app-slot"));
			for (WebElement appSlot : appSlots) {
				LocalTime time = LocalTime.parse(appSlot.findElement(By.xpath(".//div[@class='slot-time']")).getText(), DateTimeFormatter.ofPattern("HH:mm", LOCALE));
				String place = appSlot.findElement(By.xpath(".//div[@class='clinicName']")).getText();
				String doctor = appSlot.findElement(By.xpath(".//div[@class='doctorName']")).getText();
				
				slots.add(new Slot(date, time, place, doctor));
			}
		}
		
		for (Slot slot : slots) {
			System.out.println(String.format("date: [%s] time: [%s] place: [%s] doctor: [%s]", slot.getDate(), slot.getTime(), slot.getPlace(), slot.getDoctor()));
			System.out.println();
		}
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		driver.quit();
	}

	private void displayAvailableSlots(String specialty, String issue) throws InterruptedException, BotException {
		driver.get(MEDICOVER_ONLINE_BASE_URL + "/pfm-menu/");

		clickDesiredLinkFromH3Parent(specialty, issue);
		clickDesiredLinkFromH3Parent(MAKE_APPOINTMENT_H3_TAG_TEXT, MAKE_APPOINTMENT_LINK_TEXT);
		waitForSearchButton();
		clickButton(SEARCH_AVAILABLE_APPOINTMENTS_BUTTON_TEXT);
		loadAllAvailableSlots();
	}

	private void loadAllAvailableSlots() {
		while (true) {
			wait.until(ExpectedConditions.invisibilityOfElementLocated(DIV_BLOCKING_FORM));
			wait.until(ExpectedConditions.invisibilityOfElementLocated(DIV_BLOCKING_SEARCH_RESULTS));
			try {
				clickButton(SHOW_MORE_BUTTON_TEXT);
			} catch (BotException e) {
				if (BotException.ErrorCodes.DOM_CHANGED.equals(e.getErrorCode()))
				// no more slots available for loading
				break;
			}
		}
	}

	private void waitForSearchButton() throws InterruptedException, BotException {
		try {
			wait.until(ExpectedConditions.invisibilityOfElementLocated(DIV_BLOCKING_FORM));
			wait.until(ExpectedConditions.visibilityOfElementLocated(DIV_BLOCKING_FORM));
			wait.until(ExpectedConditions.invisibilityOfElementLocated(DIV_BLOCKING_FORM));
		} catch (TimeoutException e) {
			// hectic behavior - blocking div occurs initially, then disappears and appears again
			// Selenium sometimes do not catch this and see it only once, so try waiting for all the events and ignore if not all were caught
		}
	}

	private void clickButton(String buttonText) throws BotException {
		for (WebElement button : driver.findElements(By.tagName("button"))) {
			if (buttonText.equals(button.getText())) {
				try {
					wait.until(ExpectedConditions.visibilityOf(button)).click();
					return;
				} catch (TimeoutException | ElementClickInterceptedException e) {
					throw new BotException(String.format("Search form did not load in the alloted amount of seconds: [%d]", timeout), BotException.ErrorCodes.TIMEOUT);
				}
			}
		}
		throw new BotException(String.format("Could not find a search appointment button with text: [%s]", SEARCH_AVAILABLE_APPOINTMENTS_BUTTON_TEXT), BotException.ErrorCodes.DOM_CHANGED);
	}

	private void clickDesiredLinkFromH3Parent(String h3Text, String linkText) throws BotException {
		try {
			getH3TagParentByText(h3Text).findElement(By.linkText(linkText)).click();
		} catch (NoSuchElementException e) {
			throw new BotException(String.format("Could not find a link with the text: [%s] within paragraph with title: [%s]", linkText, h3Text), BotException.ErrorCodes.DOM_CHANGED);
		}
	}

	private WebElement getH3TagParentByText(String h3text) throws BotException {
		List<WebElement> h3NodesList = driver.findElements(By.tagName("h3"));
		for (WebElement h3Node : h3NodesList) {
			if (h3text.equals(h3Node.getText())) {
				// get all links under parent (div) node
				return h3Node.findElement(By.xpath("./.."));
			}
		}
		throw new BotException(String.format("Could not find any paragraph with the title: [%s]", h3text), BotException.ErrorCodes.DOM_CHANGED);
	}

	private void login() throws InterruptedException, BotException {
		WebElement logInDialogOpenButton = openPopupLoginWindow();
		switchToPopupLoginWindow();
		authenticate();
		switchBackToMainWindow(logInDialogOpenButton);
	}

	private WebElement openPopupLoginWindow() {
		driver.get(MEDICOVER_ONLINE_BASE_URL + "/Users/Account/AccessDenied");
		
		WebElement logInDialogOpenButton = driver.findElement(By.id("oidc-submit"));
		logInDialogOpenButton.click();
		return logInDialogOpenButton;
	}

	private void switchToPopupLoginWindow() throws BotException {
		Set<String> windowHandlesSet = driver.getWindowHandles();

		if (windowHandlesSet.size() != 2) {
			throw new BotException("Unexpected window handles size", BotException.ErrorCodes.DOM_CHANGED);
		}

		// get rid of the starting page, retain only the popup window
		windowHandlesSet.remove(driver.getWindowHandle());
		driver.switchTo().window(windowHandlesSet.iterator().next());
	}

	private void switchBackToMainWindow(WebElement stalePageElement) throws InterruptedException, BotException {
		int loops = 0;
		while(driver.getWindowHandles().size() != 1) {
			Thread.sleep(SLEEP_STEP);
			if (loops++ > maxRetries) {
				throw new BotException(String.format("Login action was not processed after following number of seconds: [%d]", maxRetries * SLEEP_STEP / 1000), BotException.ErrorCodes.TIMEOUT);
			}
		}

		driver.switchTo().window(driver.getWindowHandles().iterator().next());
		waitForMainWindowRefresh(stalePageElement);
	}

	private void waitForMainWindowRefresh(WebElement stalePageElement) throws InterruptedException {
		wait.until(ExpectedConditions.stalenessOf(stalePageElement));
		
		closeModalNewsPopups();
	}

	private void closeModalNewsPopups() throws InterruptedException {
		List<WebElement> modalNewsPopupButtons = driver.findElements(By.xpath("//div[@class='modal-dialog']//button[@data-dismiss='modal']"));

		for (WebElement modalNewsPopupButton : modalNewsPopupButtons) {
			closeModalNewsPopup(modalNewsPopupButton);
		}
	}

	private void closeModalNewsPopup(WebElement modalNewsPopupButton) throws InterruptedException {
		int loops = 0;
		while(!modalNewsPopupButton.isDisplayed()) {
			Thread.sleep(SLEEP_STEP);
			if (loops++ > maxRetries) {
				Logger.warn(String.format("Modal button was not displayed after following number of seconds: [%d]", maxRetries * SLEEP_STEP / 1000));
				// ignore and hope for the best
				break;
			}
		}
		if (modalNewsPopupButton.isDisplayed()) {
			modalNewsPopupButton.click();
		}
	}

	private void authenticate() throws InterruptedException, BotException {
		WebElement userNameField = driver.findElement(By.id("UserName"));
		WebElement passwordField = driver.findElement(By.id("Password"));
		WebElement loginButton = driver.findElement(By.id("loginBtn"));

		userNameField.sendKeys(username);
		passwordField.sendKeys(password);

		//wait for the javascript listener to kick in
		int loops = 0;
		while (loginButton.getAttribute("disabled") != null) {
			passwordField.clear();
			passwordField.sendKeys(password);
			Thread.sleep(SLEEP_STEP);
			if (loops++ > maxRetries) {
				throw new BotException(String.format("Login button was not yet enabled after following number of seconds: [%d]", maxRetries * SLEEP_STEP / 1000), BotException.ErrorCodes.TIMEOUT);
			}
		}
		loginButton.click();
	}

	private static WebDriver setupFirefoxDriver() {
		WebDriverManager.firefoxdriver().setup();
		
		FirefoxOptions firefoxOptions = new FirefoxOptions();
		firefoxOptions.setAcceptInsecureCerts(false);

		WebDriver driver = new FirefoxDriver(firefoxOptions);

		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
		return driver;
	}

	public static void errorExit(BotException e) {
		Logger.error(e.getMessage());
		Logger.log(String.format("Exiting: [%d]", e.getErrorCode().ordinal()));
		System.exit(e.getErrorCode().ordinal());
	}

}
