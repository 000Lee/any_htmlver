# 애니파이브 전자결재 HTML 파싱 및 DB 마이그레이션 프로젝트

다운로드한 HTML 파일을 기준으로 필요한 정보만 크롤링하여 DB에 저장하고 `.cmds` 파일을 생성하는 프로젝트입니다.

---

## 📌 주요 이슈

### 이슈 1: 초기 버전과 개선 버전
- **루트 경로의 파이썬 파일**: JSON 저장 방식 (초기 버전)
- **연도별 폴더의 파이썬 파일**: DB 저장 방식으로 개선

### 이슈 2: 결재선 데이터 정확도 개선
- 초기에는 결재의견에서 결재선을 가져왔으나, 결재의견과 결재선이 항상 일치하지 않음을 발견
- 숫자 뒤에 `_`가 붙는 파일명의 파이썬 코드가 이를 수정하기 위해 추가된 코드

### 이슈 3: 인사정보 CSV 필수
- 모든 폴더 안에는 인사정보 CSV 파일(새로운 조직도)이 있어야 함

### 이슈 4: 참조문서, 결재선 크롤링은 java로 실행함
- 참조문서 이슈 폴더 안에 AnyFiveReferenceDocCrawler.java와 AnyFiveActiviesCrawler.java로 크롤링을 하고
- 참조문서의 경우에는 참조문서 이슈폴더의 참조문서를DB에 적재.ipynb를 실행
- 결재선의 경우에는 각각의 연도별 폴더의 10_결재선시간DB버전.ipynb을 실행
---

## 📂 프로젝트 구조
```
any_htmlVer_all/
├── 참조문서 이슈/
│   ├── 숫자제목 파이썬코드 (cmds에서 references 존재하는 문서의 sourceId 숫자 추출)
│   ├── 참조문서 (AnyFiveReferenceDocCrawler.java) - Java 크롤링 → DB 적재
│   ├── 결재선 (AnyFiveActiviesCrawler.java) - Java 크롤링 → DB 적재
│   └── 참조문서를 DB에 적재 (reference_documents 테이블 → documents 테이블의 references로 적용)
│
├── 결재의견 동일인물.ipynb
│   └── HTML 파일에서 같은 사람이 의견을 여러 번 낸 경우의 파일을 보여주는 코드
│
└── YYYY-MM-DD~YYYY-MM-DD/ (연도별 폴더)
    ├── 인사정보_부서코드추가.csv
    └── 스크립트들...
```

---

## 🔄 작업 순서

### 1(htmlConvert).ipynb
HTML의 기안일, 보존연한을 변환

---

### 2(htmlToJsonDB).ipynb
전자결재 HTML 파일을 파싱하여 JSON 파일 + MariaDB에 저장

**주의:** 이 코드로 모든 필드를 채울 수 없음. 크롤링해야 하는 항목들과 뒤에서 보강해야 하는 항목들이 있음

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (파일 위치, end_year 설정, DB 설정)

---

### 3(htmlToDocbody_convert).ipynb
HTML 파일에 CSS를 인라인 삽입하고 압축하여 DB에 저장

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (파일 위치, end_year 설정, DB 설정)

---

### 4(isPublic_crawling).ipynb
전자결재 시스템에서 "공개" 문서 목록을 크롤링하여 수집 후 DB에 저장

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (end_year 설정, 조회 기간, DB 설정)

**주의:** 날짜 설정은 5년 이하로 해야 함. 미리 설정해보고 경고창 뜨면 쪼개서 실행하기

#### 실행 후 DB에서 아래 SQL 실행 (예시: 2010년, 숫자 변경 필요)
```sql
USE any_approval;

-- public 테이블을 documents에 적용시키기
UPDATE documents 
SET is_public = 1 
WHERE source_id IN (
    SELECT 문서ID FROM public WHERE end_year = 2010
)
AND end_year = 2010;

-- is_public이 1인 모든 문서 조회
SELECT source_id, title, is_public, end_year
FROM documents
WHERE is_public = 1
AND end_year = 2010;
```

---

### 5(form_crawling).ipynb
전자결재 시스템에서 문서 목록(그리드)을 페이지별로 크롤링. 상세 페이지 진입 없이 목록만 수집하여 DB에 저장

**용도:** 양식명 얻기 위한 크롤링

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (기간 설정, DB 설정)

#### 실행 후 DB에서 아래 SQL 실행 (예시: 2010년, 숫자 변경 필요)
```sql
-- form을 documents 테이블에 적용

-- 몇 건이 업데이트될 건지 확인
SELECT COUNT(*) AS 업데이트될_개수
FROM documents d
JOIN form f ON d.source_id = f.문서ID 
WHERE d.end_year = 2010 AND f.end_year = 2010;

-- 실제 데이터 미리보기
SELECT
    d.source_id,
    d.form_name AS 현재값,
    f.양식명 AS 업데이트될값,
    d.end_year
FROM documents d
JOIN form f ON d.source_id = f.문서ID
WHERE d.end_year = 2010 AND f.end_year = 2010
LIMIT 10;

-- 업데이트 실행
USE any_approval;
UPDATE documents d
JOIN form f ON d.source_id = f.문서ID
SET d.form_name = f.양식명
WHERE d.end_year = 2010 AND f.end_year = 2010;
```

---

### 6(NewUser_insert).ipynb
CSV 파일의 인사정보를 읽어서 DB documents 테이블의 사원 정보 업데이트

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (CSV 파일 경로 설정, DB 설정)

---

### 7(이미지경로수정 + 복사본 다운).ipynb
documents 테이블의 doc_body에 포함된 이미지 경로를 변환하고 다운로드 폴더의 이미지 파일을 새 위치로 복사

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (DB 설정, end_year, 이미지 원본 경로, 복사본 경로, src 접두사)

**주의:** 2010, 2015년도에는 실행 X
- 2010: 이미지가 없음
- 2015: 다운로드 파일에 없는, 수동으로 받은 이미지 1개만 존재

---

### 8(첨부경로수정 + 다운).ipynb
documents 테이블의 attaches 컬럼에 포함된 첨부파일 경로를 변환하고 첨부파일을 새 위치로 복사

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (DB 설정, end_year, 첨부파일 원본 경로, 복사본 경로, src 접두사)

---

### 9_결재선결재의견수정.ipynb
결재선을 HTML 파일의 상단에서 가져옴 & 새로운 조직도 매칭 & 결재의견

**주의:** 
- 결재선과 결재의견의 개수가 일치하지 않는 경우가 많으므로 결재의견은 뒤에서 보강
- 기안자의 시간은 HTML에 있지만 결재자들의 시간은 없기에 00:00:00으로 처리하고 뒤에서 보강

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (폴더 경로, end_year, 인사정보 CSV 파일, DB 설정)

---

### 10_결재선시간DB버전.ipynb
시간 크롤링한 테이블을 document 테이블에 연결

**처리 방식:**
- 크롤링 오류가 있을 수도 있으니 두 테이블 간의 제목을 비교하여 불일치 시 건너뛰고 적재 (불일치될 경우 따로 파일 생성)
- activities에서 type과 name이 모두 일치할 경우 actionDate 업데이트

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (DB 설정)

---

### 11_이름시간으로 결재의견넣기DB.ipynb
DB 결재선의 이름, 시간과 HTML 파일의 결재의견 쪽의 이름, 시간이 일치하면 DB에 결재의견을 넣는 코드

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (폴더 경로, end_year, DB 설정)

---

### 12_9(DB에서 cmds로 변환).ipynb
DB documents 테이블에서 특정 연도 문서를 조회하여 addDocument 명령어 형식의 `.cmds` 파일로 내보내기

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (DB 설정, end_year, 출력 파일명)

---

### 13_cmds 파일에서 img 태그를 제거하고 sourceId를 기록.ipynb
`//office\.anyfive\.com/[^>]+>` 부분 삭제

**수정 필요:** `#여기를 수정하세요` 주석 부분을 상황에 맞게 변경 (파일 경로 설정)

---

## 🛠️ 수동으로 수정해야 할 것들

### 1. 추가로 수동으로 더 삭제하거나 경로를 따로 바꿔줘야 하는 경우도 있음
→ 각 연도별 폴더 내부의 removed_sourceIds 또는 이미지 경로 수정 목록에 저장해둠

### 2. 중복 `<style>` 태그 삭제
`<style>` 두 개 들어가는 게 있음
- 두 번째 스타일 태그 삭제 (2016년도 초쯤까지 `<STYLE>`로 HTML 파일에 태그가 들어가 있음)

### 3. CSS 선택자 삭제
CSS에서 `html, body` 선택자 삭제하기

### 4. Notepad++ 정규식 작업

**첨부파일 경로, 이미지 경로**
- 첨부파일의 경우 모두 추가해야하지만, 이미지의 경우 다른 연도는 `7(이미지경로수정~)`에 경로를 추가하는 코드가 있어서 2025년만 따로 추가해주기
- 경로 맨 앞에 `/PMS_SITE-U7OI43JLDSMO/approval/` 붙이기

**doc_ _숫자 붙이기**
- 패턴: `("sourceId":\s*")(\d+)(")`
- 치환: `\1doc_\2_숫자\3`

---

## 💡 참고사항

- 각 스크립트 내부의 `#여기를 수정하세요` 주석을 확인하여 환경에 맞게 설정 변경
- 연도별로 작업 시 `end_year` 값을 일관되게 변경
- SQL 실행 시 연도 조건(`WHERE end_year = YYYY`) 반드시 확인
