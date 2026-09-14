# Spring Flow MVP

## 1. 프로젝트 목적

Spring 프로젝트의 소스 코드를 분석하여  
**Controller 메서드를 시작점으로 Java 메서드 간 호출 관계를 다이어그램으로 시각화하는 웹 서비스**를 만든다.

사용자가 Git Repository URL을 입력하면 서버가 Repository를 임시 디렉토리에 clone하고,
Spring 프로젝트의 Java 소스를 정적 분석한다.

분석 결과는 아래와 같은 형태로 표현한다.

```text
┌──────────────────────────────────┐
│ POST /user/insert                │
│ UserController.userInsert()      │
└──────────────────────────────────┘
                  │
          ┌───────┴────────┐
          │                │
          ▼                ▼
┌────────────────────┐ ┌────────────────────┐
│ UserMstService     │ │ UserDtlService     │
│ insert()           │ │ insert()           │
└────────────────────┘ └────────────────────┘
```

노드를 클릭하면:

1. 해당 메서드의 소스 코드를 확인한다.
2. 해당 메서드가 호출하는 하위 메서드를 펼칠 수 있다.

MVP에서는 우선 **Java 메서드 호출 관계**까지 구현한다.

MyBatis XML / SQL 분석은 MVP 이후 기능으로 둔다.

---

# 2. 확정 기술 스택

## Backend

- Java 17
- Spring Boot 4.0.8
- Gradle
- WAR Packaging
- Spring Web MVC
- Thymeleaf
- Validation
- MyBatis
- PostgreSQL Driver
- JavaParser
- JavaSymbolSolver

## Frontend

- Thymeleaf
- HTML
- CSS
- Vanilla JavaScript
- Cytoscape.js

React / Vue / Angular는 사용하지 않는다.

## Database

- Neon PostgreSQL

## Deployment

- AWS EC2
- Nginx
- External Tomcat
- Spring Boot WAR
- Neon PostgreSQL

---

# 3. 개발 스타일

이 프로젝트는 DTO / VO 중심 구조로 작성하지 않는다.

Controller / Service / Mapper 사이의 주요 데이터 전달은 아래 타입을 사용한다.

```java
Map<String, Object>
List<Map<String, Object>>
```

예:

```java
@PostMapping("/api/projects/analyze")
@ResponseBody
public Map<String, Object> analyze(
        @RequestBody Map<String, Object> param) {

    return projectService.analyze(param);
}
```

Service:

```java
public Map<String, Object> analyze(Map<String, Object> param) {

    Map<String, Object> result = new HashMap<>();

    String repositoryUrl = (String) param.get("repositoryUrl");

    result.put("repositoryUrl", repositoryUrl);

    return result;
}
```

Mapper:

```java
@Mapper
public interface ProjectMapper {

    int insertProject(Map<String, Object> param);

    Map<String, Object> selectProject(Map<String, Object> param);

    List<Map<String, Object>> selectProjectList(Map<String, Object> param);
}
```

MyBatis XML:

```xml
<select id="selectProject" parameterType="map" resultType="map">
    SELECT
        id,
        name,
        repository_url AS repositoryUrl,
        status
    FROM projects
    WHERE id = #{projectId}
</select>
```

## Map 사용 원칙

- Request DTO를 만들지 않는다.
- Response DTO를 만들지 않는다.
- VO를 만들지 않는다.
- JPA Entity를 사용하지 않는다.
- Controller / Service / Mapper는 Map 기반으로 작성한다.
- 목록은 `List<Map<String, Object>>`를 사용한다.
- Map Key는 camelCase를 사용한다.
- 동일한 의미의 Key 이름은 프로젝트 전체에서 통일한다.
- 불필요하게 깊은 중첩 Map 구조는 만들지 않는다.

JavaParser가 제공하는 아래 타입은 그대로 사용한다.

```text
CompilationUnit
MethodDeclaration
MethodCallExpr
ClassOrInterfaceDeclaration
```

이들은 프로젝트의 DTO/VO가 아니므로 Map으로 변환할 필요가 없다.

---

# 4. WAR 패키징

프로젝트는 실행형 JAR가 아니라 WAR로 패키징한다.

Gradle 기본 방향:

```gradle
plugins {
    id 'java'
    id 'war'
    id 'org.springframework.boot' version '4.0.8'
    id 'io.spring.dependency-management'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {

    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter'

    runtimeOnly 'org.postgresql:postgresql'

    implementation 'com.github.javaparser:javaparser-symbol-solver-core'

    providedRuntime 'org.springframework.boot:spring-boot-starter-tomcat'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

실제 dependency version은 Spring Boot 4.0.8 및 사용하는 라이브러리의 호환 버전에 맞춰 설정한다.

Main Application Class는 외부 Tomcat 배포를 위해 `SpringBootServletInitializer`를 상속한다.

```java
@SpringBootApplication
public class SpringFlowApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(
            SpringApplicationBuilder application) {

        return application.sources(SpringFlowApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringFlowApplication.class, args);
    }
}
```

빌드:

```bash
./gradlew clean bootWar
```

결과:

```text
build/libs/spring-flow.war
```

---

# 5. 전체 구조

```text
Browser
   │
   ▼
Nginx
   │
   ▼
Tomcat
   │
   ▼
Spring Boot WAR
   │
   ├── Thymeleaf
   ├── REST API
   ├── Git Clone
   ├── JavaParser
   ├── JavaSymbolSolver
   └── MyBatis
          │
          ▼
    Neon PostgreSQL
```

---

# 6. MVP 핵심 기능

MVP에서 반드시 구현한다.

1. Git Repository URL 입력
2. Repository clone
3. Spring Controller 탐색
4. RequestMapping 계열 Annotation 분석
5. Controller 메서드 목록 추출
6. Controller 메서드 내부 Method Call 분석
7. Service → Service 호출 분석
8. Service → Mapper 호출 분석
9. Method Call Graph 생성
10. Cytoscape.js로 그래프 표시
11. Node 클릭 시 Method Source 표시
12. Node 클릭 또는 Expand 버튼으로 하위 호출 펼치기
13. 분석 결과 Neon PostgreSQL 저장
14. 분석 완료 후 clone 디렉토리 삭제

---

# 7. MVP 제외 범위

초기 MVP에서는 구현하지 않는다.

- MyBatis XML 분석
- SQL 분석
- DB Table 분석
- Spring AOP 흐름 분석
- Runtime Proxy 분석
- Reflection 호출 추적
- Kafka 분석
- Redis 분석
- 외부 API 호출 분석
- 분석 대상 프로젝트 실행
- 분석 대상 프로젝트 Maven / Gradle Build
- Private Repository 인증
- 회원가입
- GitHub OAuth
- GitHub Webhook
- 자동 Repository 동기화
- 다중 브랜치 비교

---

# 8. 사용자 흐름

## 8.1 Repository 입력

메인 화면:

```text
Git Repository URL

[ https://github.com/example/sample.git ]

[ Analyze ]
```

## 8.2 분석

```text
Git URL 입력
    ↓
projects 등록
    ↓
analysis_runs 등록
    ↓
UUID Workspace 생성
    ↓
Git Clone
    ↓
Java Source 탐색
    ↓
Spring Component 분석
    ↓
Method 분석
    ↓
Method Call 분석
    ↓
DB 저장
    ↓
Workspace 삭제
    ↓
프로젝트 분석 화면 이동
```

---

# 9. Workspace

Repository는 EC2 내부의 임시 Workspace에 clone한다.

예:

```text
/opt/spring-flow/workspace/{UUID}
```

예:

```text
/opt/spring-flow/workspace/
└── 550e8400-e29b-41d4-a716-446655440000/
```

Repository 이름을 그대로 Workspace 이름으로 사용하지 않는다.

Clone:

```bash
git clone --depth 1 {repositoryUrl} {workspace}
```

반드시 `finally`에서 Workspace를 삭제한다.

```java
Path workspace = workspaceService.create();

try {

    gitCloneService.cloneRepository(repositoryUrl, workspace);

    projectAnalysisService.analyze(projectId, workspace);

} finally {

    workspaceService.delete(workspace);
}
```

---

# 10. 보안 원칙

Clone 받은 Repository의 코드는 절대 실행하지 않는다.

금지:

```text
./gradlew build
./gradlew bootRun
mvn package
mvn spring-boot:run
java -jar
npm install
npm run
```

허용:

```text
git clone
↓
파일 읽기
↓
AST Parsing
↓
정적 분석
```

추가 정책:

- Git URL validation
- HTTP/HTTPS Git URL만 우선 허용
- clone timeout
- Repository 최대 크기 제한
- Workspace path traversal 방지
- 분석 시간 제한
- 동시 분석 작업 수 제한
- Shell Injection 방지
- 사용자가 입력한 URL을 명령 문자열에 직접 이어붙이지 않는다.

---

# 11. 분석 대상

기본적으로 Repository 내 모든 `src/main/java`를 탐색한다.

예:

```text
module-user/src/main/java
module-order/src/main/java
module-common/src/main/java
```

멀티모듈 프로젝트도 가능한 범위에서 분석한다.

---

# 12. Spring Component 분석

아래 Annotation을 기준으로 클래스 역할을 구분한다.

## CONTROLLER

```java
@Controller
@RestController
```

## SERVICE

```java
@Service
```

## REPOSITORY

```java
@Repository
```

## COMPONENT

```java
@Component
```

## MAPPER

```java
@Mapper
```

그 외:

```text
OTHER
```

---

# 13. RequestMapping 분석

아래 Annotation을 분석한다.

```java
@RequestMapping
@GetMapping
@PostMapping
@PutMapping
@PatchMapping
@DeleteMapping
```

Class Level Mapping과 Method Level Mapping을 조합한다.

예:

```java
@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping("/insert")
    public void userInsert() {
    }
}
```

결과:

```text
POST /user/insert
```

그래프 Node:

```text
POST /user/insert

UserController
userInsert()
```

---

# 14. Method 분석

Method마다 아래 정보를 수집한다.

```text
packageName
className
methodName
parameterTypes
returnType
filePath
startLine
endLine
sourceCode
componentType
httpMethod
requestPath
```

Method Key는 단순 methodName을 사용하지 않는다.

Method Overloading을 구분하기 위해 아래 형태를 사용한다.

```text
packageName.className#methodName(parameterType1,parameterType2)
```

예:

```text
com.example.user.UserService#insert(User)
```

---

# 15. Method Call 분석

예:

```java
@PostMapping("/insert")
public void userInsert(User user) {

    userMstService.insert(user);
    userDtlService.insert(user);
}
```

결과:

```text
UserController.userInsert()
    ├── UserMstService.insert()
    └── UserDtlService.insert()
```

각 호출은 Edge로 저장한다.

```text
callerMethodId
calleeMethodId
callOrder
resolved
calleeDisplayName
```

---

# 16. Call Order

가능하면 Source 내 Method 호출 순서를 저장한다.

예:

```java
validate();
saveMaster();
saveDetail();
sendMessage();
```

결과:

```text
1 validate
2 saveMaster
3 saveDetail
4 sendMessage
```

단, 정적 분석이므로 실제 Runtime 실행 순서를 완벽히 보장하지 않는다.

예:

```java
if (...) {
    serviceA.run();
} else {
    serviceB.run();
}
```

그래프는 Runtime Trace가 아니라 **Static Call Graph**이다.

---

# 17. JavaParser

Java Method 분석에 정규식을 사용하지 않는다.

JavaParser를 사용한다.

주요 AST:

```text
CompilationUnit
ClassOrInterfaceDeclaration
MethodDeclaration
MethodCallExpr
FieldDeclaration
ObjectCreationExpr
AnnotationExpr
```

예:

```java
CompilationUnit unit =
        parser.parse(file)
              .getResult()
              .orElseThrow();

unit.findAll(MethodDeclaration.class)
    .forEach(method -> {

        method.findAll(MethodCallExpr.class)
              .forEach(call -> {

              });
    });
```

---

# 18. JavaSymbolSolver

가능한 범위에서 JavaSymbolSolver로 호출 대상을 resolve한다.

예:

```java
userService.insert(user);
```

목표:

```text
com.example.user.UserService.insert(User)
```

Resolve 실패는 전체 분석 실패로 처리하지 않는다.

예:

```text
resolved = false
calleeDisplayName = userService.insert()
```

분석 가능한 관계는 계속 저장한다.

---

# 19. 그래프 UI

Cytoscape.js를 사용한다.

기본 방향:

```text
TOP → BOTTOM
```

예:

```text
                 Controller
                     │
             ┌───────┴───────┐
             ▼               ▼
          Service A       Service B
             │
             ▼
           Mapper
```

---

# 20. 그래프 초기 표시

프로젝트의 모든 Method를 한 번에 표시하지 않는다.

처음에는 다음 흐름으로 탐색한다.

```text
Controller 목록
    ↓
API 목록
    ↓
API 선택
    ↓
Controller Method + Direct Child 표시
```

예:

```text
POST /user/insert
        │
   ┌────┴────┐
   ▼         ▼
Service A  Service B
```

---

# 21. Node 클릭

노드를 클릭하면 오른쪽 Detail Panel에 정보를 표시한다.

예:

```text
UserMstService.insert()

Type
SERVICE

File
src/main/java/com/example/user/UserMstService.java

Line
35 ~ 52

Calls
- validate()
- userMapper.insert()

Source

public void insert(User user) {
    ...
}
```

---

# 22. 하위 호출 펼치기

Node 선택 후:

```text
[ Expand Calls ]
```

버튼을 제공한다.

또는 더블 클릭을 사용할 수 있다.

예:

```text
UserMstService.insert()
         │
    ┌────┴────────┐
    ▼             ▼
validate()   UserMapper.insert()
```

이미 표시된 Node는 중복 생성하지 않는다.

---

# 23. 화면 구조

```text
┌──────────────────────────────────────────────────────────────┐
│ Spring Flow                                      Repository │
├──────────────┬─────────────────────────────┬─────────────────┤
│ Controllers  │                             │ Method Detail   │
│              │                             │                 │
│ User         │         Graph Area          │ Type            │
│ Controller   │                             │ Class           │
│              │                             │ Method          │
│ Order        │                             │ File            │
│ Controller   │                             │ Calls           │
│              │                             │ Source Code     │
└──────────────┴─────────────────────────────┴─────────────────┘
```

왼쪽:

```text
Controller
API
```

중앙:

```text
Cytoscape Graph
```

오른쪽:

```text
선택 Method 상세
Source Code
```

---

# 24. 화면

## `/`

기능:

```text
Git URL 입력
Analyze
최근 분석 Project 목록
```

## `/projects/{projectId}`

기능:

```text
Controller 목록
API 목록
Graph
Method Detail
```

---

# 25. API

## 분석 요청

```http
POST /api/projects/analyze
```

Request:

```json
{
  "repositoryUrl": "https://github.com/example/sample.git"
}
```

Response:

```json
{
  "projectId": 1,
  "analysisId": 10,
  "status": "ANALYZING"
}
```

---

## 프로젝트 조회

```http
GET /api/projects/{projectId}
```

Response Type:

```java
Map<String, Object>
```

---

## Controller 목록

```http
GET /api/projects/{projectId}/controllers
```

Response Type:

```java
List<Map<String, Object>>
```

예:

```json
[
  {
    "classId": 10,
    "className": "UserController",
    "apis": [
      {
        "methodId": 101,
        "httpMethod": "POST",
        "requestPath": "/user/insert",
        "methodName": "userInsert"
      }
    ]
  }
]
```

---

## Method Call 조회

```http
GET /api/methods/{methodId}/calls
```

해당 Method의 Direct Child만 반환한다.

Response:

```json
{
  "nodes": [
    {
      "id": "101",
      "className": "UserController",
      "methodName": "userInsert",
      "componentType": "CONTROLLER",
      "httpMethod": "POST",
      "requestPath": "/user/insert"
    },
    {
      "id": "201",
      "className": "UserMstService",
      "methodName": "insert",
      "componentType": "SERVICE"
    }
  ],
  "edges": [
    {
      "source": "101",
      "target": "201",
      "order": 1
    }
  ]
}
```

Response Type:

```java
Map<String, Object>
```

---

## Method 상세

```http
GET /api/methods/{methodId}
```

Response:

```json
{
  "id": 201,
  "packageName": "com.example.user",
  "className": "UserMstService",
  "methodName": "insert",
  "componentType": "SERVICE",
  "filePath": "src/main/java/com/example/user/UserMstService.java",
  "startLine": 35,
  "endLine": 52,
  "sourceCode": "public void insert(...) { ... }"
}
```

---

# 26. DB

## projects

```text
id
name
repository_url
default_branch
commit_hash
status
created_at
analyzed_at
```

Status:

```text
PENDING
CLONING
ANALYZING
COMPLETED
FAILED
```

---

## analysis_runs

```text
id
project_id
status
started_at
finished_at
error_message
```

---

## source_classes

```text
id
project_id
package_name
class_name
qualified_name
component_type
file_path
```

component_type:

```text
CONTROLLER
SERVICE
REPOSITORY
MAPPER
COMPONENT
OTHER
```

---

## source_methods

```text
id
class_id
method_name
signature
return_type
start_line
end_line
source_code
http_method
request_path
```

---

## method_calls

```text
id
project_id
caller_method_id
callee_method_id
callee_display_name
call_order
resolved
```

`callee_method_id`는 Resolve 실패 시 NULL 가능.

---

# 27. DB 관계

```text
projects
  │
  ├── analysis_runs
  │
  └── source_classes
          │
          └── source_methods
                  │
                  └── method_calls
                       ├── caller_method_id
                       └── callee_method_id
```

DB 접근은:

```text
Controller
    ↓
Service
    ↓
MyBatis Mapper Interface
    ↓
MyBatis XML
    ↓
Neon PostgreSQL
```

구조로 한다.

---

# 28. 패키지 구조

```text
com.springflow
├── project
│   ├── controller
│   ├── service
│   └── mapper
│
├── analysis
│   ├── service
│   │   ├── ProjectAnalysisService
│   │   ├── SourceScanService
│   │   ├── SpringComponentAnalyzer
│   │   ├── JavaMethodAnalyzer
│   │   └── MethodCallAnalyzer
│   └── mapper
│
├── git
│   ├── GitCloneService
│   └── WorkspaceService
│
├── graph
│   ├── controller
│   ├── service
│   └── mapper
│
└── common
```

불필요한 `dto`, `vo`, `entity` 패키지를 만들지 않는다.

---

# 29. Resource 구조

```text
src/main/resources
├── templates
│   ├── index.html
│   └── project.html
│
├── static
│   ├── css
│   │   └── app.css
│   └── js
│       ├── project.js
│       └── graph.js
│
├── mapper
│   ├── project
│   │   └── ProjectMapper.xml
│   ├── analysis
│   │   └── AnalysisMapper.xml
│   └── graph
│       └── GraphMapper.xml
│
└── application.yml
```

Cytoscape 관련 로직은 가능한 `graph.js`로 분리한다.

---

# 30. Service 책임

## GitCloneService

```text
Git URL 검증
Repository clone
clone timeout
```

## WorkspaceService

```text
UUID Workspace 생성
Workspace 삭제
경로 관리
```

## SourceScanService

```text
src/main/java 탐색
Java File 목록 반환
```

## SpringComponentAnalyzer

```text
@Controller
@RestController
@Service
@Repository
@Component
@Mapper
```

분류.

## JavaMethodAnalyzer

```text
Method 추출
Method Source 추출
Line 추출
RequestMapping 분석
```

## MethodCallAnalyzer

```text
MethodCallExpr 추출
caller → callee 분석
Symbol Resolution
```

## GraphService

```text
Method Direct Child 조회
Map 기반 nodes / edges 생성
Cytoscape Response 반환
```

---

# 31. MyBatis 규칙

Mapper Interface:

```java
@Mapper
public interface ProjectMapper {

    int insertProject(Map<String, Object> param);

    Map<String, Object> selectProject(Map<String, Object> param);

    List<Map<String, Object>> selectProjectList(
            Map<String, Object> param
    );
}
```

XML:

```xml
<mapper namespace="com.springflow.project.mapper.ProjectMapper">

    <select
        id="selectProject"
        parameterType="map"
        resultType="map">

        SELECT
            id,
            name,
            repository_url AS "repositoryUrl",
            status
        FROM projects
        WHERE id = #{projectId}

    </select>

</mapper>
```

SQL은 XML에 작성한다.

Annotation 기반 SQL은 사용하지 않는다.

예:

```java
@Select(...)
```

사용하지 않는다.

---

# 32. 분석 실패 처리

하나의 Java File 파싱 실패 때문에 프로젝트 전체 분석을 중단하지 않는다.

예:

```text
File A SUCCESS
File B FAILED
File C SUCCESS
```

가능한 범위까지 분석한다.

Warning을 기록한다.

예:

```text
PARSE_FAILED
SYMBOL_RESOLVE_FAILED
UNSUPPORTED_SOURCE
```

---

# 33. Logging

다음 로그를 남긴다.

```text
분석 시작
Repository URL
Clone 시작
Clone 완료
Java File 개수
Class 개수
Method 개수
Method Call 개수
Unresolved Method Call 개수
분석 완료 시간
분석 실패 이유
Workspace 삭제 결과
```

Source Code 전체를 로그로 출력하지 않는다.

---

# 34. MVP 개발 순서

## Phase 1 - 기본 프로젝트

구현:

- Spring Boot 4.0.8
- Java 17
- Gradle
- WAR
- Spring Web MVC
- Thymeleaf
- Validation
- MyBatis
- PostgreSQL
- Neon 연결 구조
- Main Page
- 기본 Package
- MyBatis 설정
- `SpringBootServletInitializer`

완료 조건:

```text
프로젝트 실행 성공
Thymeleaf index.html 표시
Neon 연결 성공
WAR Build 성공
```

---

## Phase 2 - Git Clone

구현:

- Git URL 입력
- Workspace 생성
- Repository Shallow Clone
- Clone Timeout
- Workspace 삭제

완료 조건:

```text
Public Git Repository Clone 가능
```

---

## Phase 3 - Controller 탐색

구현:

- JavaParser
- Java File Scan
- Controller Annotation 분석
- RequestMapping 분석
- DB 저장

완료 조건:

```text
POST /user/insert
GET /user/{id}
```

목록 표시.

---

## Phase 4 - Method 분석

구현:

- Class Method 분석
- Method Signature 생성
- Method Source 저장
- Line 저장

완료 조건:

```text
Class → Method 조회 가능
```

---

## Phase 5 - Method Call 분석

구현:

- MethodCallExpr 추출
- JavaSymbolSolver
- Caller / Callee 관계 저장

완료 조건:

```text
UserController.userInsert()
        ↓
UserService.insert()
```

DB 저장.

---

## Phase 6 - Graph

구현:

- Cytoscape.js
- Controller API 선택
- Direct Child 조회
- Graph 표시

완료 조건:

```text
Controller
   ├── Service A
   └── Service B
```

표시.

---

## Phase 7 - Node Expand

구현:

- Method Node 선택
- `/api/methods/{methodId}/calls`
- Child Node 추가
- Duplicate Node 방지

완료 조건:

```text
Controller
   ↓
Service
   ↓
Mapper
```

점진적 확장.

---

## Phase 8 - Source Viewer

구현:

- Node 클릭
- Method Detail API
- 오른쪽 Source Panel

완료 조건:

```text
Node 클릭
→ 해당 Method Source 표시
```

---

# 35. MVP 완료 시나리오

예제:

```java
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserMstService userMstService;
    private final UserDtlService userDtlService;

    @PostMapping("/insert")
    public void userInsert(User user) {

        userMstService.insert(user);
        userDtlService.insert(user);
    }
}
```

Repository URL 입력 후:

```text
POST /user/insert
```

API가 목록에 표시된다.

클릭:

```text
┌──────────────────────────────┐
│ POST /user/insert            │
│ UserController.userInsert()  │
└──────────────────────────────┘
              │
      ┌───────┴────────┐
      ▼                ▼
┌────────────────┐ ┌────────────────┐
│ UserMstService │ │ UserDtlService │
│ insert()       │ │ insert()       │
└────────────────┘ └────────────────┘
```

`UserMstService.insert()` 클릭:

```text
오른쪽에 Source Code 표시
```

Expand:

```text
UserMstService.insert()
       │
       ▼
UserMapper.insert()
```

여기까지 동작하면 MVP 완료로 본다.

---

# 36. MVP 이후

## V1.1 MyBatis XML

```text
UserMapper.insert()
       ↓
UserMapper.xml
       ↓
<insert id="insert">
```

## V1.2 SQL

```text
Mapper
 ↓
XML
 ↓
SQL
```

## V1.3 Table

```text
POST /user/insert
 ↓
Controller
 ↓
Service
 ↓
Mapper
 ↓
SQL
 ↓
USER_MST
```

## V1.4 Impact Analysis

역방향 Call Graph:

```text
UserMapper.select()
      ↑
UserService.get()
      ↑
GET /user/{id}
```

목표:

```text
이 메서드를 수정하면 어떤 API가 영향을 받는가?
```

---

# 37. Codex 작업 규칙

Codex는 반드시 아래 규칙을 따른다.

1. `Spring Boot 4.0.8`을 사용한다.
2. `Java 17`을 사용한다.
3. Gradle을 사용한다.
4. WAR로 패키징한다.
5. 외부 Tomcat 배포가 가능한 구조로 만든다.
6. Frontend는 Thymeleaf + Vanilla JavaScript를 사용한다.
7. React / Vue / Angular를 사용하지 않는다.
8. Graph는 Cytoscape.js를 사용한다.
9. DB는 PostgreSQL 기준으로 작성한다.
10. Production DB는 Neon PostgreSQL을 사용한다.
11. DB 접근은 MyBatis Mapper + XML 방식으로 구현한다.
12. SQL은 MyBatis XML에 작성한다.
13. JPA를 도입하지 않는다.
14. JPA Entity를 만들지 않는다.
15. Spring Data Repository를 만들지 않는다.
16. Request DTO를 만들지 않는다.
17. Response DTO를 만들지 않는다.
18. VO를 만들지 않는다.
19. 데이터 전달은 `Map<String, Object>`를 기본으로 한다.
20. 목록은 `List<Map<String, Object>>`를 사용한다.
21. Map Key는 camelCase를 사용한다.
22. Java Source 분석은 JavaParser를 사용한다.
23. Method Call 분석을 정규식으로 구현하지 않는다.
24. 가능한 경우 JavaSymbolSolver를 사용한다.
25. Symbol Resolution 실패가 전체 분석 실패로 이어지면 안 된다.
26. Clone한 Repository의 코드를 실행하지 않는다.
27. Clone한 Repository에서 Maven / Gradle Build를 실행하지 않는다.
28. 분석 완료 또는 실패 후 Workspace를 반드시 삭제한다.
29. 불필요한 Framework를 추가하지 않는다.
30. 과도한 추상화를 피한다.
31. Phase 단위로 구현한다.
32. 각 Phase 완료 후 실행 가능한 상태를 유지한다.
33. MVP 완성을 최우선으로 한다.

---

# 38. Codex 첫 작업

먼저 **Phase 1만 구현한다.**

구현 대상:

```text
Spring Boot 4.0.8
Java 17
Gradle
WAR
Spring Web MVC
Thymeleaf
Validation
MyBatis
PostgreSQL Driver
Neon 연결용 설정
SpringBootServletInitializer
기본 Package 구조
기본 MyBatis 설정
index.html
```

환경변수:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

`application.yml`에 실제 DB Password를 작성하지 않는다.

예:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

Phase 1 완료 후 아래 내용을 보고한다.

```text
1. 생성한 파일
2. 수정한 파일
3. 주요 구현 내용
4. 실행 방법
5. WAR Build 방법
6. 확인해야 할 사항
7. 다음 Phase에서 할 작업
```

Phase 1 완료 전에는 Phase 2를 구현하지 않는다.

---

# 39. 프로젝트 핵심 원칙

이 서비스는 일반적인 Class Dependency Diagram을 만드는 것이 목적이 아니다.

개발자가 실제 유지보수 업무에서 확인하고 싶은:

```text
RequestMapping
      ↓
Controller Method
      ↓
Service Method
      ↓
Service Method
      ↓
Mapper Method
```

흐름을 빠르게 파악할 수 있게 만드는 것이 목적이다.

전체 프로젝트를 한 화면에 모두 그리지 않는다.

특정 API 또는 특정 Method를 시작점으로 필요한 부분을 점진적으로 펼친다.

최종 MVP 목표:

> Git URL 입력 → Clone → Spring 코드 분석 → API 선택 → Method Call Graph 표시 → Node 클릭 시 Source 확인
