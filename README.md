# PC Manager Java

자바로 만드는 피시방 관리 시스템 지침서입니다.
Swing 이용
고객 입장은 실제 로컬 TCP 소켓 통신으로 처리
현재 단계에서는 서버, 고객, 카운터를 별도 프로세스로 실행

## 1. 프로젝트 목표

실제 피시방 운영에 가까운 흐름을 가진 관리 시스템을 만든다.

핵심 목표:

- 고객 PC에서 상품 주문 가능
- 고객과 카운터 간 통신 메시지 가능
- 로그인한 좌석의 남은 시간 확인 가능
- 관리자에서 좌석, 회원, 결제, 주문, 시간 연장 관리 가능
- 추후 DB, 네트워크, 결제 모듈을 붙여도 구조가 무너지지 않게 설계

## 2. 주요 사용자

### 고객

- 회원 로그인 또는 비회원 사용
- 남은 시간 확인
- 시간 연장 요청
- 음식/음료 주문
- 카운터 호출
- 1:1 문의 메시지 전송

### 카운터 직원

- 전체 좌석 상태 확인
- 주문 접수 및 처리
- 고객 문의 확인 및 응답
- 시간 충전/연장 처리
- 입실/퇴실 처리

### 관리자

- 상품 등록/수정
- 회원 관리
- 매출 통계
- 공지사항 관리
- 시스템 설정

## 3. 핵심 기능 목록

### 3.1 좌석 관리

- 좌석 번호별 상태 표시
- 상태 종류:
  - 사용 가능
  - 사용 중
  - 청소 중
  - 점검 중
- 좌석별 현재 사용자, 시작 시간, 종료 예정 시간 조회

### 3.2 회원/비회원 관리

- 회원 가입
- 로그인
- 회원 등급 관리
- 포인트 적립
- 비회원 임시 사용 등록
- 비회원 닉네임 입장

### 3.3 시간 관리

- 선불 시간 충전
- 후불 사용 시간 계산
- 남은 시간 실시간 표시
- 종료 10분/5분/1분 전 알림
- 시간 종료 시 자동 잠금 또는 연장 안내

### 3.4 상품 주문

- 카테고리별 상품 조회
- 장바구니
- 주문 요청
- 주문 상태 변경
- 주문 취소 가능 여부 처리

주문 상태 예시:

- `REQUESTED`
- `ACCEPTED`
- `PREPARING`
- `DELIVERING`
- `COMPLETED`
- `CANCELED`

### 3.5 통신 메시지

- 고객 -> 카운터 메시지 전송
- 카운터 -> 고객 답변
- 호출 유형 구분

메시지 유형 예시:

- `CALL_STAFF`
- `ASK_TIME`
- `ORDER_INQUIRY`
- `TECH_SUPPORT`
- `GENERAL_CHAT`

### 3.6 결제

- 현금
- 카드
- 회원 잔액
- 포인트 사용

### 3.7 관리자 기능

- 상품 재고 관리
- 공지 팝업 전송
- 강제 로그아웃
- 강제 종료/잠금
- 일별 매출 조회

## 4. 추천 기술 방향

처음에는 데스크톱 기반으로 시작하는 것이 구현 난이도와 관리 측면에서 가장 현실적이다.

권장 1차 구성:

- 언어: Java 17 이상
- UI: JavaFX
- DB: MySQL 또는 MariaDB
- ORM/접근: JPA 또는 MyBatis
- 빌드: IDE(IntelliJ) 또는 javac
- 통신: TCP Socket
- 직렬화: JSON
- 로깅: SLF4J + Logback

학습 목적이고 빠르게 만드는 것이 우선이면:

- UI: Java Swing
- DB: SQLite
- 통신: 로컬 TCP Socket 서버를 앱 내부에 띄우고 고객 화면은 소켓 클라이언트로 접속
- 실행 구조: `ServerMain`, `CustomerMain`, `CounterMain` 분리

## 5. 권장 아키텍처

초기부터 UI와 비즈니스 로직을 분리한다.

### 계층 구조

- `presentation`
  - 화면, 컨트롤러, 입력 처리
- `application`
  - 주문, 시간연장, 메시지 전송 같은 유스케이스
- `domain`
  - 핵심 엔티티와 규칙
- `infrastructure`
  - DB, 소켓, 외부 API, 파일 저장

### 권장 패키지 구조

```text
src/main/java/com/pcmanager
├── Main.java
├── common
│   ├── config
│   ├── exception
│   ├── util
│   └── constant
├── domain
│   ├── seat
│   ├── member
│   ├── order
│   ├── product
│   ├── payment
│   ├── message
│   └── time
├── application
│   ├── seat
│   ├── member
│   ├── order
│   ├── payment
│   ├── message
│   └── admin
├── infrastructure
│   ├── persistence
│   ├── network
│   ├── security
│   └── notification
└── presentation
    ├── customer
    ├── counter
    └── admin
```

## 6. 핵심 도메인 설계

### Seat

- `seatId`
- `seatNumber`
- `status`
- `currentUserId`
- `loginTime`
- `expectedEndTime`

### Member

- `memberId`
- `loginId`
- `password`
- `name`
- `phone`
- `remainingMinutes`
- `point`
- `grade`

### Product

- `productId`
- `categoryId`
- `name`
- `price`
- `stock`
- `saleStatus`

### Order

- `orderId`
- `seatId`
- `memberId`
- `orderItems`
- `totalPrice`
- `orderStatus`
- `requestedAt`

### Message

- `messageId`
- `seatId`
- `senderType`
- `messageType`
- `content`
- `readYn`
- `sentAt`

### Payment

- `paymentId`
- `targetType`
- `targetId`
- `paymentMethod`
- `amount`
- `paidAt`

## 7. DB 테이블 초안

최소 테이블:

- `seats`
- `members`
- `products`
- `product_categories`
- `orders`
- `order_items`
- `messages`
- `payments`
- `time_charges`
- `notices`

관계 예시:

- `orders` 1:N `order_items`
- `members` 1:N `orders`
- `seats` 1:N `orders`
- `seats` 1:N `messages`

## 8. 화면 구성 제안

### 고객 화면

- 로그인 화면
- 메인 홈
- 남은 시간 패널
- 상품 주문 화면
- 메시지/호출 화면
- 공지사항 팝업

### 카운터 화면

- 전체 좌석 현황판
- 주문 관리 패널
- 메시지 응답 패널
- 회원 검색/충전 화면
- 매출 처리 화면

### 관리자 화면

- 상품 관리
- 회원 관리
- 통계 대시보드
- 시스템 설정

## 9. 통신 메시지 규격 예시

소켓 통신은 최종적으로 JSON 형태로 통일하는 것이 좋다.

```json
{
  "type": "ORDER_CREATE",
  "requestId": "req-20260310-0001",
  "seatId": 12,
  "memberId": 1001,
  "payload": {
    "items": [
      { "productId": 1, "quantity": 2 }
    ]
  },
  "sentAt": "2026-03-10T10:00:00"
}
```

메시지 타입 예시:

- `LOGIN`
- `LOGOUT`
- `REMAINING_TIME_REQUEST`
- `TIME_EXTEND_REQUEST`
- `ORDER_CREATE`
- `ORDER_CANCEL`
- `MESSAGE_SEND`
- `NOTICE_PUSH`
- `SEAT_STATUS_UPDATE`

현재 MVP 구현은 텍스트 기반 프로토콜로 먼저 동작한다.

- 고객 입장 요청: `ENTER|nickname`
- 서버 성공 응답: `OK|입장 완료|seatId|seatNumber|nickname`
- 서버 실패 응답: `ERROR|reason`

현재 적용된 실제 흐름:

1. 고객이 Swing 화면에서 닉네임 입력
2. 고객 화면이 로컬 소켓 서버(`127.0.0.1:5050`)로 입장 요청 전송
3. 서버가 빈 좌석을 찾아 배정
4. 카운터 화면 좌석 현황에 즉시 반영
5. 배정된 좌석 기준으로 주문/메시지/남은 시간 관리

추가로 현재 구현된 소켓 명령:

- `PRODUCTS`
- `SEAT|seatId`
- `ALL_SEATS`
- `ORDER|seatId|productId|quantity`
- `ORDERS|seatId`
- `ALL_ORDERS`
- `ADVANCE_ORDER|orderId`
- `MESSAGE|seatId|messageType|base64Content`
- `REPLY|seatId|base64Content`
- `MESSAGES|seatId`
- `ALL_MESSAGES`
- `EXTEND|seatId|minutes`

## 10. 업무 흐름 예시

### 상품 주문 흐름

1. 고객이 상품 선택
2. 고객 PC에서 주문 요청 전송
3. 카운터 서버가 주문 생성
4. 카운터 화면에 새 주문 표시
5. 직원이 주문 수락
6. 조리/준비 상태 변경
7. 전달 완료 처리

### 메시지 흐름

1. 고객이 문의 유형 선택
2. 메시지 입력 후 전송
3. 카운터 화면 알림 발생
4. 직원 응답 작성
5. 고객 화면에 답변 표시

### 시간 관리 흐름

1. 로그인 시 시작 시간 기록
2. 분 단위 또는 초 단위로 남은 시간 계산
3. 임계 시간에서 알림 표시
4. 시간이 0이 되면 잠금 또는 연장 요청 팝업 표시

## 11. 개발 우선순위

처음부터 전부 만들지 말고 아래 순서로 나누는 것이 맞다.

### 1단계: 로컬 단일 프로그램

- 좌석 관리
- 닉네임 기반 입장
- 남은 시간 계산
- 상품 목록/주문
- 메시지 등록
- 로컬 TCP 소켓 입장 처리

목표:

- DB 없이 메모리 기반으로 먼저 동작

### 2단계: DB 연동

- 회원, 상품, 주문 저장
- 로그인 데이터 유지
- 주문 내역 조회

### 3단계: 실제 통신

- 고객 프로그램과 카운터 프로그램 분리
- TCP 소켓 통신 연결
- 실시간 주문/메시지 반영
- 고객 화면은 서버 메모리를 직접 참조하지 않음
- 카운터 화면도 서버 스냅샷을 소켓으로 조회

### 4단계: 관리자 기능 강화

- 매출 통계
- 재고 관리
- 공지 전송
- 권한 분리

## 12. 구현 시 중요한 규칙

- UI 코드에서 DB 직접 접근 금지
- 주문/결제/시간 계산은 서비스 계층에서 처리
- 상태값은 문자열 하드코딩 대신 `enum` 사용
- 시간 계산은 `LocalDateTime`, `Duration` 사용
- 금액은 `int` 또는 `long`으로 관리
- 비밀번호는 평문 저장 금지
- 예외 메시지와 사용자 표시 메시지 분리

## 13. 추천 enum 예시

- `SeatStatus`
- `OrderStatus`
- `MessageType`
- `PaymentMethod`
- `UserRole`
- `SaleStatus`

## 14. 최소 MVP 범위

처음 완성 목표는 아래 정도가 적당하다.

- 닉네임 기반 고객 입장
- 소켓 서버를 통한 좌석 입실
- 남은 시간 표시
- 상품 주문
- 카운터 주문 확인
- 고객/카운터 메시지 송수신
- 고객 앱 / 카운터 앱 분리 실행

이 범위까지만 먼저 완성하면 "피시방 관리 시스템처럼 보이는" 핵심은 나온다.

## 15. 다음 작업 추천

바로 시작하려면 아래 순서로 진행한다.

1. Java 프로젝트 구조 생성
2. `domain`, `application`, `presentation` 패키지 생성
3. 좌석/회원/주문/메시지 도메인 클래스 작성
4. 메모리 기반 저장소 구현
5. 고객 화면, 카운터 화면 초안 구현
6. 이후 DB와 소켓 통신 연결

## 16. 추가로 만들면 좋은 문서

- `docs/requirements.md`
- `docs/use-cases.md`
- `docs/db-schema.md`
- `docs/protocol.md`
- `docs/ui-wireframe.md`

원하면 다음 단계로 바로

- 자바 프로젝트 기본 폴더 구조 생성
- 클래스 뼈대 생성
- DB 스키마 초안 생성
- 고객용/카운터용 화면 설계

까지 이어서 만들 수 있다.

## 17. 실행 방법

서버를 먼저 실행한 뒤 고객/카운터를 각각 실행한다.

```bash
java -cp src/main/java com.pcmanager.ServerMain
```

```bash
java -cp src/main/java com.pcmanager.CustomerMain
```

```bash
java -cp src/main/java com.pcmanager.CounterMain
```

권장 다음 단계:

1. 주문/메시지 응답을 JSON 프로토콜로 교체
2. SQLite 또는 MySQL 저장소 연결
3. 회원 로그인과 비회원 닉네임 입장 분기
4. 서버와 클라이언트 간 지속 연결 또는 이벤트 푸시 도입
