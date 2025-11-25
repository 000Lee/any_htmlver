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
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AnyFiveReferenceDocCrawler {

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

    // 데이터 삽입 SQL 쿼리 (원본문서ID, 참조문서ID)
    private static final String INSERT_SQL =
            "INSERT INTO reference_documents (source_document_id, reference_document_id) VALUES (?, ?)";

    // 참조문서 div ID
    private static final String REF_DOC_DIV_ID = "aprDocShowAddDocList";

    // 정규식 패턴: aprDocShow.openRefDoc('숫자')에서 숫자 추출
    private static final Pattern REF_DOC_PATTERN = Pattern.compile("aprDocShow\\.openRefDoc\\('(\\d+)'\\)");


    public static void main(String[] args) {
        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--blink-settings=imagesEnabled=false");
        // options.addArguments("--headless"); // UI 없이 실행하려면 이 옵션을 활성화하세요.

        WebDriver driver = null;
        Connection conn = null;

        // ★★★ 문서 ID 목록 (텍스트로 제공) ★★★
        List<String> documentIds = new ArrayList<>();

        // 2021-2025
        documentIds.addAll(Arrays.asList(
                "15377405", "15391835", "15484981", "17030834", "17157636", "17247829", "17246591", "17263540",
                "17355763", "17420914", "17421902", "17440378", "17442254", "17445854", "17675020", "17892228",
                "18276233", "18279273", "18297624", "18304305", "18320049", "18404495", "18418040", "18427539",
                "18429651", "18595767", "18601926", "18670896", "18683603", "18772897", "18809947", "18876965",
                "18893374", "18973237", "19069183", "19069245", "19610483", "19632799", "19645981", "19666759",
                "19687351", "19687360", "19683800", "19701387", "19703975", "19709405", "19725126", "19725099",
                "19761906", "19867408", "20263582", "20461924", "20502471", "20699385", "20963888", "20995786",
                "21010763", "21044874", "21310847", "21334081", "21334090", "21343542", "21449034", "21465446",
                "21465438", "21983374", "22447751", "22485119", "22677031", "22706769", "22797214", "22960773",
                "23040737", "23154453", "23205291", "23206477", "23204664", "23302014", "23302365", "23301968",
                "23301617", "23296190", "23296265", "23440395", "23434564", "23586050", "23598045", "23593806",
                "23593621", "23785702", "23786125", "23816456", "23816343", "24030637", "24036384", "24122224",
                "24149687", "24276629", "24349498", "24376425", "24376677", "24382857", "24414332", "24414785",
                "24423508", "24456977", "24517437", "24604862", "24711851", "24709545", "24772794", "24785317",
                "24951043", "25176456", "25175780", "25193105", "25217840", "25487102", "25524388", "25555996",
                "25600099", "25608317", "25677979", "25710095", "25709546", "25709558", "25773712", "25780892",
                "25807039", "25814135", "25944887", "25969348", "26019086", "26072608", "26088043", "26163747",
                "26163450", "26163386", "26204142", "26203048", "26235538", "26291955", "26312747", "26322351",
                "26332959", "26350336", "26349877", "26413296"
        ));

        // 2016-2020
        documentIds.addAll(Arrays.asList(
                "2009508", "2009576", "2009625", "2009645", "2009740", "2981791", "3009758", "3010538",
                "3052701", "3071392", "3079097", "3076077", "3085548", "3085569", "3085559", "3088931",
                "3094529", "3091905", "3140656", "3172683", "3181517", "3183490", "3185859", "3190547",
                "3196683", "3222958", "3242908", "3248394", "3252498", "3266610", "3269765", "3273511",
                "3292325", "3316750", "3315791", "4205516", "5085502", "5124421", "5586779", "5620417",
                "5629439", "5641421", "5777742", "5807329", "5828386", "5830858", "5864443", "5897821",
                "5898072", "5913576", "5951790", "5966323", "5967517", "6055814", "6055871", "6089548",
                "6091980", "6115443", "6135443", "6143218", "6156934", "6194767", "6216976", "6216799",
                "6225013", "6227259", "6243392", "6244212", "6278118", "6278170", "6297708", "6315207",
                "6379123", "6383908", "6536129", "6536155", "6552040", "6563472", "6608648", "6653909",
                "6709462", "6729487", "6737224", "6760318", "6769794", "6768323", "6776305", "6783870",
                "6795885", "6821505", "6860052", "6886098", "6886123", "6935861", "6939929", "6977202",
                "6998842", "7005368", "7037040", "7038637", "7050680", "7052272", "7082945", "7083041",
                "7149391", "7153668", "7152006", "7153704", "7213965", "7213996", "7249682", "7257915",
                "7270702", "7280893", "7277739", "7288154", "7288080", "7343314", "7344084", "7356060",
                "7371392", "7376466", "7422768", "7428047", "7486008", "7512428", "7510482", "7533724",
                "7535328", "7561264", "7561282", "7561362", "7594499", "7687518", "7691525", "7734467",
                "7768806", "7768823", "7768865", "7796686", "7858931", "7858043", "7894886", "7906330",
                "7909555", "7914912", "7943472", "7941010", "7979431", "8012657", "8035743", "8063628",
                "8063652", "8110729", "8126421", "8212256", "8250987", "8301473", "8301520", "8419273",
                "8431395", "8439528", "8439560", "8529220", "8553635", "8562669", "8585294", "8639438",
                "8639474", "8782475", "8782556", "8782520", "8780710", "8819556", "8924130", "8922484",
                "8930204", "8953794", "8971258", "8972447", "9196803", "9196793", "9271709", "9370308",
                "9421378", "9477610", "9521839", "9654467", "9654473", "9674026", "9798406", "9822667",
                "9853610", "9994218", "10002753", "10019691", "10121473", "10514006", "10630649", "10762617",
                "10806678", "10827785", "10921534", "10934514", "11861495", "11859413", "12723088", "13020288",
                "13477815", "13611717", "13864321", "14160239", "14379678", "14832231", "14956840", "15078648",
                "15155817"
        ));

        // 2011-2015
        documentIds.addAll(Arrays.asList(
                "2006082", "2006085", "2006241", "2006353", "2006489", "2006592", "2006994", "2007001",
                "2007003", "2007176", "2007384", "2007516", "2007524", "2007659", "2007780", "2007975",
                "2008119", "2008334", "2008413", "2008445", "2008563", "2008586", "2008670", "2008785",
                "2008789", "2008870", "2008932", "2008985", "2009012", "2009013", "2009047", "2009160",
                "2009169", "2009479", "2009492"
        ));

        System.out.println("총 " + documentIds.size() + "개의 문서 ID 로드 완료.");

        try {
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
            System.out.println("  > 'managementDocList' 객체 로드 확인됨.");
            System.out.println("  > '결재문서관리' 메뉴 클릭 완료.");

            // ---------------------------------------------
            // 3단계: 건별 데이터 추출 루프
            // ---------------------------------------------

            // DB 연결 설정
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            conn.setAutoCommit(false);
            System.out.println("3단계: DB 연결 성공 (참조문서 삽입 준비).");

            PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL);
            int totalInsertedRows = 0;
            int documentsWithReferences = 0;
            int documentsWithoutReferences = 0;

            // 추출한 ID 목록을 순회하며 크롤링 시작
            for (int idx = 0; idx < documentIds.size(); idx++) {
                String docId = documentIds.get(idx);
                int currentDocRows = 0; // 현재 문서에서 추출된 참조문서 수

                try {
                    System.out.println("\n  > [" + (idx + 1) + "/" + documentIds.size() + "] 문서 ID: " + docId + " 처리 중...");

                    // 3-1. JS 함수 호출로 상세 페이지 로드 (Iframe 내에서 실행)
                    driver.switchTo().defaultContent();
                    wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(IFRAME_NAME));
                    Thread.sleep(2000);

                    String clickFunctionCall = String.format("managementDocList.clickGridRow(%s);", docId);
                    js.executeScript(clickFunctionCall);
                    Thread.sleep(2000);
                    System.out.println("  > JS 함수 호출 성공: " + clickFunctionCall);

                    // 3-2. 참조문서 div 찾기 (없을 수도 있음)
                    List<String> refDocIds = new ArrayList<>();

                    try {
                        // 참조문서 div가 존재하는지 확인
                        WebElement refDocDiv = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(REF_DOC_DIV_ID)));

                        // li 요소들 찾기
                        List<WebElement> liElements = refDocDiv.findElements(By.tagName("li"));

                        if (liElements.isEmpty()) {
                            System.out.println("  > 참조문서 없음 (li 요소 없음)");
                        } else {
                            // 각 li의 a 태그에서 onclick 속성 추출
                            for (WebElement li : liElements) {
                                try {
                                    WebElement aTag = li.findElement(By.tagName("a"));
                                    String onclickAttr = aTag.getAttribute("onclick");

                                    if (onclickAttr != null) {
                                        // 정규식으로 숫자 추출
                                        Matcher matcher = REF_DOC_PATTERN.matcher(onclickAttr);
                                        if (matcher.find()) {
                                            String refDocId = matcher.group(1);
                                            refDocIds.add(refDocId);
                                            System.out.println("    - 참조문서 ID 발견: " + refDocId);
                                        }
                                    }
                                } catch (Exception aEx) {
                                    // a 태그가 없는 경우 무시
                                }
                            }
                        }

                    } catch (Exception divEx) {
                        System.out.println("  > 참조문서 div 없음 또는 찾을 수 없음");
                    }

                    // 3-3. DB에 저장
                    if (refDocIds.isEmpty()) {
                        documentsWithoutReferences++;
                    } else {
                        documentsWithReferences++;

                        // 각 참조문서 ID를 DB에 삽입
                        for (String refDocId : refDocIds) {
                            pstmt.setString(1, docId);
                            pstmt.setString(2, refDocId);
                            pstmt.addBatch();
                            currentDocRows++;
                            totalInsertedRows++;
                        }
                    }

                    // 문서 처리 완료 후 즉시 배치 실행 및 커밋
                    if (currentDocRows > 0) {
                        pstmt.executeBatch();
                        conn.commit();
                        System.out.println("  > 배치 실행 및 커밋 완료. (" + currentDocRows + " 참조문서 저장)");
                    }

                    // 3-4. 목록 페이지로 복귀 (Iframe 내에서 작동)
                    driver.navigate().back();
                    System.out.println("  > 목록 페이지로 복귀.");

                    // 복귀 후, Iframe 콘텐츠가 다시 목록 뷰로 갱신되기를 기다립니다.
                    Thread.sleep(1000);

                } catch (Exception processEx) {
                    System.err.println("  > 오류: 문서 처리 실패 (" + docId + "): " + processEx.getMessage());

                    // 오류 발생 시, 안전하게 목록 페이지로 복귀 시도
                    try {
                        driver.navigate().back();
                        Thread.sleep(1000);
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
            // 4단계: 최종 처리 확인
            // ---------------------------------------------
            System.out.println("\n4단계: 모든 DB 작업이 완료되었습니다.");
            System.out.println("  > 총 " + documentIds.size() + "개 문서 처리 완료");
            System.out.println("  > 참조문서가 있는 문서: " + documentsWithReferences + "개");
            System.out.println("  > 참조문서가 없는 문서: " + documentsWithoutReferences + "개");
            System.out.println("  > 총 " + totalInsertedRows + "개 참조문서 관계 DB에 저장 완료.");

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