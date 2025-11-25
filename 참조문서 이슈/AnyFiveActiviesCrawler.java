package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.JavascriptExecutor;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class AnyFiveActiviesCrawler {

    private static final String WEB_DRIVER_ID = "webdriver.chrome.driver";
    private static final String WEB_DRIVER_PATH = "C:/Users/LEEJUHWAN/Downloads/chromedriver-win64/chromedriver-win64/chromedriver.exe"; // ★★★ 이 부분을 수정해야 함 ★★★

    private static final String USER_ID = "master";
    private static final String USER_PW = "five4190any#";
    private static final String TARGET_URL = "http://office.anyfive.com";
    private static final String IFRAME_NAME = "content_frame"; // 게시판 콘텐츠 Iframe 이름

    // ★★★ MariaDB 연결 정보 ★★★
    private static final String DB_URL = "jdbc:mariadb://localhost:3306/any_approval"; // DB 주소 및 스키마
    private static final String DB_USER = "root";       // 사용자 ID
    private static final String DB_PASSWORD = "1234";   // 사용자 PW
    // ----------------------------

    // 데이터 삽입 SQL 쿼리
    private static final String INSERT_SQL =
            "INSERT INTO approval_data_2025 (document_id, post_title, sequence, status, approval_date, department, approver) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    // 문서 ID를 가져올 SQL 쿼리
// 문서 ID를 가져올 SQL 쿼리 - 이미 처리된 문서 제외
    private static final String SELECT_DOCUMENT_IDS_SQL =
            "SELECT source_id FROM document_sources " +
                    "WHERE end_year = 2025 " +
                    "AND source_id NOT IN (SELECT DISTINCT document_id FROM approval_data_2025) " +
                    "ORDER BY source_id";

    // 상세 페이지에서 문서 제목을 추출할 XPath
    private static final String DOC_TITLE_XPATH = "//span[@class='apr_title']";



    public static void main(String[] args) {
        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage"); // Linux 환경에서 유용
        options.addArguments("--disable-gpu");          // GPU 사용 비활성화
        options.addArguments("--blink-settings=imagesEnabled=false"); // 이미지 로드 비활성화 (리소스 절약)
        // options.addArguments("--headless"); // UI 없이 실행하려면 이 옵션을 활성화하세요.

        WebDriver driver = null;
        Connection conn = null;
        List<String> documentIds = new ArrayList<>();

        try {
            // --- 0. DB에서 크롤링할 문서 ID 목록 로드 ---
            System.out.println("0단계: DB에서 크롤링할 문서 ID 목록 로드 시작.");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            try (PreparedStatement pstmt = conn.prepareStatement(SELECT_DOCUMENT_IDS_SQL);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    documentIds.add(rs.getString("source_id"));
                }
            }

            if (documentIds.isEmpty()) {
                System.out.println("  > 경고: DB에서 가져온 문서 ID 목록이 비어 있습니다. 크롤링을 종료합니다.");
                return;
            }
            System.out.println("  > DB 로드 완료. 총 " + documentIds.size() + "개의 문서 ID 확인.");

            // DB 연결 해제 (재연결은 배치 작업 직전에 수행)
            if (conn != null) {
                conn.close();
                conn = null;
            }
            // ---------------------------------------------


            // 1. WebDriver 및 크롤링 환경 설정
            driver = new ChromeDriver(options);
            driver.manage().window().maximize();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // ---------------------------------------------
            // 2단계: 로그인 및 메뉴 클릭을 통한 게시판 진입
            // ---------------------------------------------

            System.out.println("2단계: 로그인 및 메뉴 클릭을 통한 게시판 진입 시도.");

            // 2-1. 로그인 페이지 접속
            driver.get(TARGET_URL);

            // 2-2. 로그인 처리
            driver.findElement(By.id("uid")).sendKeys(USER_ID);
            driver.findElement(By.id("pwd")).sendKeys(USER_PW);
            driver.findElement(By.className("btn_login")).click();
            System.out.println("  > 로그인 버튼 클릭 완료.");

            // 2-3. '다음에 변경하기' 팝업 닫기
            String xpathPassButton = "//button[@class='home' and @onclick='pwChangePass.pass()']";
            By passButtonSelector = By.xpath(xpathPassButton);
            WebElement passButton = wait.until(ExpectedConditions.elementToBeClickable(passButtonSelector));
            passButton.click();
            System.out.println("  > '다음에 변경하기' 팝업 닫기 완료.");

            // Iframe 전환: 메뉴가 Iframe 내부에 있으므로 클릭 전에 전환합니다.
            driver.switchTo().defaultContent();
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(IFRAME_NAME));
            System.out.println("  > 메뉴 클릭을 위해 Iframe [" + IFRAME_NAME + "]으로 전환 성공.");

            // 2-4. '전자결재' 아이콘 클릭 (메뉴 로드)
            System.out.println("  > '전자결재' 아이콘 클릭 시도...");
            By aprLinkSelector = By.xpath("//a[contains(@href, '/apr/') and contains(@class, 'left_menu')]");
            WebElement aprLink = wait.until(ExpectedConditions.elementToBeClickable(aprLinkSelector));
            js.executeScript("arguments[0].click();", aprLink); // JS 강제 클릭
            Thread.sleep(1000); // 메뉴 로드 대기
            System.out.println("  > '전자결재' 아이콘 클릭 완료.");

            // 2-5. '결재문서관리' 메뉴 클릭 (managementDoc 로드)
            System.out.println("  > '결재문서관리' 메뉴 클릭 시도...");
            By docLiSelector = By.xpath("//li[contains(@onclick, 'managementDoc')]");
            WebElement managementDocLi = wait.until(ExpectedConditions.elementToBeClickable(docLiSelector));
            js.executeScript("arguments[0].click();", managementDocLi); // JS 강제 클릭

            // managementDocList 객체가 정의될 때까지 명시적으로 기다림
            System.out.println("  > JavaScript 객체 로딩 대기 중...");
//            wait.until(driver -> (Boolean) js.executeScript("return typeof managementDocList !== 'undefined'"));
            System.out.println("  > 'managementDocList' 객체 로드 확인됨.");

            System.out.println("  > '결재문서관리' 메뉴 클릭 완료.");

            // ---------------------------------------------
            // 3단계: 건별 데이터 추출 루프
            // ---------------------------------------------

            // DB 연결 설정 (배치 작업 직전에 재연결)
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            conn.setAutoCommit(false);
            System.out.println("3단계: DB 재연결 성공 (건별 삽입 준비).");

            PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL);
            int totalInsertedRows = 0;

            // 추출한 ID 목록을 순회하며 크롤링 시작
            for (String postId : documentIds) {
                String postTitle = "[제목 추출 실패]";
                int currentPostRows = 0; // 현재 문서에서 추출된 행 수

                try {
                    System.out.println("\n  > 문서 ID: " + postId + " 처리 중...");

                    // 3-1. JS 함수 호출로 상세 페이지 로드 (Iframe 내에서 실행)
                    driver.switchTo().defaultContent();
                    wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(IFRAME_NAME));
                    Thread.sleep(2000);

                    String clickFunctionCall = String.format("managementDocList.clickGridRow(%s);", postId);
                    js.executeScript(clickFunctionCall);
                    Thread.sleep(2000);
                    System.out.println("  > JS 함수 호출 성공: " + clickFunctionCall);

                    // 3-2. 상세 페이지 로드 대기 (Iframe 내용이 갱신되기를 기다림)
                    By tableSelector = By.id("apprLineTable");
                    WebElement apprLineTable = wait.until(ExpectedConditions.presenceOfElementLocated(tableSelector));
                    Thread.sleep(2000);

                    // 3-3. 문서 제목 추출
                    try {
                        WebElement titleElement = driver.findElement(By.xpath(DOC_TITLE_XPATH));
                        postTitle = titleElement.getText().trim();
                    } catch (Exception titleEx) {
                        System.err.println("  > 경고: 제목 추출 실패, 기본값 사용. 오류: " + titleEx.getMessage());
                    }

                    System.out.println("  > 결재 라인 테이블 로드 확인. (제목: " + postTitle + ")");

                    // 3-4. 결재 라인 테이블 찾기 및 데이터 추출
                    List<WebElement> rows = apprLineTable.findElements(By.tagName("tr"));

                    for (int i = 1; i < rows.size(); i++) {
                        WebElement row = rows.get(i);
                        List<WebElement> cells = row.findElements(By.tagName("td"));

                        // 데이터 추출
                        String sequence = cells.get(0).getText().trim();
                        String status = cells.get(2).getText().trim().replace("\n", " ");
                        String approvalDate = cells.get(3).getText().trim();
                        String department = cells.get(4).getText().trim();
                        String approver = cells.get(5).findElement(By.tagName("a")).getText().trim();

                        // 3-5. DB 배치에 추가
                        pstmt.setString(1, postId);
                        pstmt.setString(2, postTitle);
                        pstmt.setString(3, sequence);
                        pstmt.setString(4, status);
                        pstmt.setString(5, approvalDate);
                        pstmt.setString(6, department);
                        pstmt.setString(7, approver);

                        pstmt.addBatch();
                        totalInsertedRows++;
                        currentPostRows++;
                    }

                    // ★★★ 수정: 문서 처리 완료 후 즉시 배치 실행 및 커밋 (메모리 최적화) ★★★
                    if (currentPostRows > 0) {
                        pstmt.executeBatch(); // 배치 실행
                        conn.commit();        // 커밋
                        System.out.println("  > 배치 실행 및 커밋 완료. (" + currentPostRows + " rows inserted for " + postId + ")");
                    }
                    // ★★★ 수정 끝 ★★★

                    // 3-6. 목록 페이지로 복귀 (Iframe 내에서 작동)
                    driver.navigate().back();
                    System.out.println("  > 목록 페이지로 복귀.");

                    // 복귀 후, Iframe 콘텐츠가 다시 목록 뷰로 갱신되기를 기다립니다.
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("apprLineTable")));
                    System.out.println("  > Iframe 콘텐츠 갱신 확인 완료.");

                } catch (Exception processEx) {
                    System.err.println("  > 오류: 문서 처리 실패 (" + postId + "): " + processEx.getMessage());

                    // 오류 발생 시, 안전하게 목록 페이지로 복귀 시도
                    driver.navigate().back();
                    try {
                        // Iframe이 깨졌을 경우를 대비해 다시 전환 시도
                        driver.switchTo().defaultContent();
                        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(IFRAME_NAME));
                    } catch (Exception recoveryEx) {
                        System.err.println("  > 경고: 목록 복귀 중 치명적인 오류 발생. 루프 종료.");
                        break;
                    }
                }
            } // for loop 종료

            // ---------------------------------------------
            // 4단계: 최종 처리 확인 (이전의 대규모 배치 실행 코드는 제거되었습니다.)
            // ---------------------------------------------
            System.out.println("\n4단계: 모든 DB 작업이 완료되었습니다.");
            System.out.println("  > 총 " + totalInsertedRows + "개 행 DB에 삽입 완료.");

        } catch (SQLException e) {
            System.err.println("  > [DB 오류] 데이터베이스 작업 중 오류 발생: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("  > 롤백 실패: " + rollbackEx.getMessage());
            }
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("크롤링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 5. 자원 해제
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("DB 연결 해제 실패: " + e.getMessage());
            }
            if (driver != null) {
                // driver.quit();
                System.out.println("WebDriver 종료.");
            }
        }
    }
}